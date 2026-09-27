package dev.caveslite;

import dev.caveslite.util.TagHelper;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * "Who's killed the most custom mobs" leaderboards: one for total kills, and
 * one per mob id enabled in config.yml (off by default per mob - a server
 * only pays the cost for the mobs it actually wants ranked).
 *
 * A kill updates the in-memory count immediately. The sorted ranking that
 * placeholders read, and the on-disk copy (leaderboards.yml, its own file -
 * separate from player data), only refresh periodically and the disk write
 * happens off the main thread. That avoids the "resort + synchronous save on
 * every single kill" pattern that would cause main-thread stutters on a busy
 * server (see DragonSlayer's LeaderManager for what NOT to copy here).
 */
public final class KillLeaderboard implements Listener {
    private final Plugin plugin;
    private final File file;
    private final Map<String, NamespacedKey> mobKillKeys = new HashMap<>();

    private boolean enabled;
    private boolean totalEnabled;
    private int topSize;
    private Set<String> trackedMobs = new HashSet<>();

    // Live counts, updated immediately on kill.
    private final Map<UUID, Integer> totalKills = new HashMap<>();
    private final Map<String, Map<UUID, Integer>> perMobKills = new HashMap<>();
    private final Set<UUID> dirty = new HashSet<>();

    // Sorted snapshots refreshed periodically - what nameAt()/amountAt() (and
    // so the placeholders) actually read.
    private List<Map.Entry<UUID, Integer>> totalRanking = List.of();
    private Map<String, List<Map.Entry<UUID, Integer>>> mobRanking = new HashMap<>();

    public KillLeaderboard(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "leaderboards.yml");
    }

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        totalEnabled = cfg.getBoolean("total", true);
        topSize = Math.max(1, cfg.getInt("top-size", 10));

        trackedMobs = new HashSet<>();
        ConfigurationSection mobsSection = cfg.getConfigurationSection("mobs");
        if (mobsSection != null) {
            for (String id : mobsSection.getKeys(false)) {
                if (mobsSection.getBoolean(id, false)) {
                    trackedMobs.add(id.toLowerCase(Locale.ROOT));
                }
            }
        }
        for (String id : trackedMobs) {
            mobKillKeys.computeIfAbsent(id, k -> new NamespacedKey(plugin, "kills-" + k));
            perMobKills.computeIfAbsent(id, k -> new HashMap<>());
        }

        if (enabled) {
            load();
            refresh();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (!enabled) return;
        LivingEntity dead = event.getEntity();
        String mobId = TagHelper.getTag(dead);
        if (mobId == null) return;
        if (!(dead.getKiller() instanceof Player player)) return;

        UUID id = player.getUniqueId();
        if (totalEnabled) {
            totalKills.merge(id, 1, Integer::sum);
        }
        if (trackedMobs.contains(mobId)) {
            var container = player.getPersistentDataContainer();
            NamespacedKey key = mobKillKeys.get(mobId);
            int kills = container.getOrDefault(key, PersistentDataType.INTEGER, 0) + 1;
            container.set(key, PersistentDataType.INTEGER, kills);
            perMobKills.get(mobId).put(id, kills);
        }
        dirty.add(id);
    }

    /** Called periodically by the plugin's scheduler. Cheap when nothing changed since the last call. */
    public void tick() {
        if (!enabled || dirty.isEmpty()) return;
        dirty.clear();
        refresh();
        saveAsync();
    }

    private void refresh() {
        if (totalEnabled) {
            totalRanking = sorted(totalKills);
        }
        Map<String, List<Map.Entry<UUID, Integer>>> updated = new HashMap<>();
        for (String mobId : trackedMobs) {
            updated.put(mobId, sorted(perMobKills.getOrDefault(mobId, Map.of())));
        }
        mobRanking = updated;
    }

    private List<Map.Entry<UUID, Integer>> sorted(Map<UUID, Integer> source) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(source.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list.size() > topSize ? list.subList(0, topSize) : list;
    }

    // ------------------------------------------------------------- reading

    /** Player name at that rank ("total", or a mob id from the mobs: section), 1-based. Null if there's no one there. */
    public String nameAt(String category, int rank) {
        Map.Entry<UUID, Integer> entry = entryAt(category, rank);
        if (entry == null) return null;
        OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
        String name = player.getName();
        return name != null ? name : entry.getKey().toString();
    }

    public Integer amountAt(String category, int rank) {
        Map.Entry<UUID, Integer> entry = entryAt(category, rank);
        return entry == null ? null : entry.getValue();
    }

    private Map.Entry<UUID, Integer> entryAt(String category, int rank) {
        if (rank < 1) return null;
        List<Map.Entry<UUID, Integer>> list = "total".equals(category) ? totalRanking : mobRanking.get(category);
        if (list == null || rank > list.size()) return null;
        return list.get(rank - 1);
    }

    /** This player's own kills of one tracked mob (works offline too). 0 if that mob isn't tracked. */
    public int getMobKills(OfflinePlayer player, String mobId) {
        NamespacedKey key = mobKillKeys.get(mobId.toLowerCase(Locale.ROOT));
        if (key == null) return 0;
        return player.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    // ------------------------------------------------------------- storage

    private void load() {
        totalKills.clear();
        if (!file.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection totalSection = data.getConfigurationSection("total");
        if (totalSection != null) {
            for (String uuid : totalSection.getKeys(false)) {
                putIfValidUUID(totalKills, uuid, totalSection.getInt(uuid));
            }
        }

        for (String mobId : trackedMobs) {
            Map<UUID, Integer> map = perMobKills.computeIfAbsent(mobId, k -> new HashMap<>());
            map.clear();
            ConfigurationSection mobSection = data.getConfigurationSection("mobs." + mobId);
            if (mobSection != null) {
                for (String uuid : mobSection.getKeys(false)) {
                    putIfValidUUID(map, uuid, mobSection.getInt(uuid));
                }
            }
        }
    }

    private void putIfValidUUID(Map<UUID, Integer> map, String uuid, int value) {
        try {
            map.put(UUID.fromString(uuid), value);
        } catch (IllegalArgumentException ignored) {
            // Skip a corrupted line rather than fail the whole load.
        }
    }

    /** Writes the current counts to disk off the main thread. */
    private void saveAsync() {
        Map<UUID, Integer> totalSnapshot = Map.copyOf(totalKills);
        Map<String, Map<UUID, Integer>> perMobSnapshot = new HashMap<>();
        for (Map.Entry<String, Map<UUID, Integer>> entry : perMobKills.entrySet()) {
            perMobSnapshot.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeToDisk(totalSnapshot, perMobSnapshot));
    }

    /** Synchronous save - only meant for plugin shutdown, where there's no more "next tick" to defer to. */
    public void flush() {
        if (!enabled) return;
        writeToDisk(Map.copyOf(totalKills), perMobKills);
    }

    private void writeToDisk(Map<UUID, Integer> totalSnapshot, Map<String, Map<UUID, Integer>> perMobSnapshot) {
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : totalSnapshot.entrySet()) {
            data.set("total." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Map<UUID, Integer>> mobEntry : perMobSnapshot.entrySet()) {
            for (Map.Entry<UUID, Integer> entry : mobEntry.getValue().entrySet()) {
                data.set("mobs." + mobEntry.getKey() + "." + entry.getKey(), entry.getValue());
            }
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save leaderboards.yml", e);
        }
    }
}
