package dev.caveslite;

import dev.caveslite.util.TagHelper;
import dev.caveslite.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * Plugin-side "achievements": counts kills of any custom mob per player and
 * celebrates when they cross a configured threshold. This is not a vanilla
 * Minecraft advancement (no toast in the Advancements menu) - that would
 * require shipping a datapack alongside the plugin.
 *
 * Optionally pays money through Vault (works with CMI, EssentialsX, or any
 * other Vault-compatible economy) if the server has an economy plugin.
 */
public final class MobAchievements implements Listener {
    /** A kill-count milestone: the label shown/announced, and its money reward. */
    public record Milestone(String label, double reward) {}

    private final Plugin plugin;
    private final NamespacedKey killsKey;
    private final VaultEconomy economy;

    private boolean enabled;
    private boolean broadcast;
    private Sound sound;
    private boolean economyEnabled;
    private double perKillReward;
    private TreeMap<Integer, Milestone> thresholds = new TreeMap<>();

    public MobAchievements(Plugin plugin, VaultEconomy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.killsKey = new NamespacedKey(plugin, "custom-mob-kills");
    }

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        broadcast = cfg.getBoolean("broadcast", true);

        String soundName = cfg.getString("sound", "minecraft:ui.toast.challenge_complete");
        sound = dev.caveslite.util.Sounds.find(soundName);
        if (sound == null) {
            plugin.getLogger().log(Level.WARNING, "Unknown achievement sound: {0}", soundName);
        }

        ConfigurationSection economyCfg = cfg.getConfigurationSection("economy");
        economyEnabled = economyCfg != null && economyCfg.getBoolean("enabled", true);
        perKillReward = economyCfg == null ? 0 : Math.max(0, economyCfg.getDouble("per-kill", 0));

        thresholds = new TreeMap<>();
        ConfigurationSection thresholdsSection = cfg.getConfigurationSection("thresholds");
        if (thresholdsSection != null) {
            for (String key : thresholdsSection.getKeys(false)) {
                ConfigurationSection entry = thresholdsSection.getConfigurationSection(key);
                if (entry == null) continue;
                try {
                    thresholds.put(Integer.parseInt(key.trim()), new Milestone(
                            entry.getString("label", key),
                            Math.max(0, entry.getDouble("reward", 0))
                    ));
                } catch (NumberFormatException e) {
                    plugin.getLogger().log(Level.WARNING, "Invalid achievement threshold: {0}", key);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (!enabled) return;
        LivingEntity dead = event.getEntity();
        if (TagHelper.getTag(dead) == null) return;
        if (!(dead.getKiller() instanceof Player player)) return;

        var container = player.getPersistentDataContainer();
        int kills = container.getOrDefault(killsKey, PersistentDataType.INTEGER, 0) + 1;
        container.set(killsKey, PersistentDataType.INTEGER, kills);

        if (economyEnabled && perKillReward > 0) {
            economy.deposit(player, perKillReward);
        }

        Milestone milestone = thresholds.get(kills);
        if (milestone != null) {
            celebrate(player, kills, milestone);
        }
    }

    private void celebrate(Player player, int kills, Milestone milestone) {
        Component subtitle = Text.legacy(milestone.label());
        player.showTitle(Title.title(
                Text.legacy("&6\u00a1Logro desbloqueado!"),
                subtitle,
                Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500))
        ));
        if (sound != null) {
            player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, 1f, 1f);
        }
        if (economyEnabled && milestone.reward() > 0) {
            economy.deposit(player, milestone.reward());
        }
        if (broadcast) {
            Component announcement = Text.legacy("&7" + player.getName() + " &falcanz\u00f3 el logro: ")
                    .append(subtitle)
                    .append(Text.legacy("&7 (" + kills + " muertes)"));
            Bukkit.getServer().sendMessage(announcement);
        }
    }

    /** Kills of any custom mob this player has (works online or offline). */
    public int getKills(OfflinePlayer player) {
        return player.getPersistentDataContainer().getOrDefault(killsKey, PersistentDataType.INTEGER, 0);
    }

    /** Kill count needed to reach the next not-yet-passed threshold, or null if already past the last one. */
    public Integer nextThreshold(int kills) {
        Map.Entry<Integer, Milestone> next = thresholds.higherEntry(kills);
        return next == null ? null : next.getKey();
    }

    /** Label of the highest threshold reached so far, or null if none yet. */
    public String lastAchievementLabel(int kills) {
        Map.Entry<Integer, Milestone> last = thresholds.floorEntry(kills);
        return last == null ? null : last.getValue().label();
    }
}
