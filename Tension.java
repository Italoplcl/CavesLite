package dev.caveslite;

import dev.caveslite.mobs.MobManager;
import dev.caveslite.util.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Plays a heartbeat near players when a scary custom mob (the ones listed
 * in "tension.mobs") is nearby, speeding up as the mob gets closer.
 */
public final class Tension {
    private final Plugin plugin;
    private final MobManager mobs;
    private final Map<UUID, Integer> countdown = new HashMap<>();

    private boolean enabled;
    private Set<String> watchedIds = Set.of();
    private double radius;
    private int minInterval;
    private int maxInterval;
    private Sound sound;
    private float volume;
    private float pitch;

    public Tension(Plugin plugin, MobManager mobs) {
        this.plugin = plugin;
        this.mobs = mobs;
    }

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        radius = cfg.getDouble("radius", 16);
        minInterval = Math.max(1, cfg.getInt("min-interval-ticks", 10));
        maxInterval = Math.max(minInterval, cfg.getInt("max-interval-ticks", 60));
        volume = (float) cfg.getDouble("volume", 1);
        pitch = (float) cfg.getDouble("pitch", 1);

        Set<String> ids = new HashSet<>();
        for (String id : cfg.getStringList("mobs")) {
            ids.add(id.toLowerCase(Locale.ROOT));
        }
        watchedIds = ids;

        String name = cfg.getString("sound", "minecraft:block.sculk_sensor.clicking");
        sound = Sounds.find(name);
        if (sound == null) {
            plugin.getLogger().log(Level.WARNING, "Unknown tension sound in config: {0}", name);
        }
    }

    /** Called periodically (every tick of the tension task) by the scheduler. */
    public void tick() {
        if (!enabled || sound == null || watchedIds.isEmpty()) return;

        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                double nearestSquared = nearestWatchedMobDistanceSquared(player);
                UUID id = player.getUniqueId();

                if (Double.isNaN(nearestSquared)) {
                    countdown.remove(id);
                    continue;
                }

                int remaining = countdown.getOrDefault(id, 0) - 1;
                if (remaining > 0) {
                    countdown.put(id, remaining);
                    continue;
                }

                double closeness = 1 - Math.min(1, Math.sqrt(nearestSquared) / radius);
                int interval = (int) Math.round(maxInterval - (maxInterval - minInterval) * closeness);
                countdown.put(id, interval);

                player.playSound(player.getLocation(), sound, SoundCategory.AMBIENT, volume,
                        pitch + (float) (closeness * 0.3));
            }
        }
    }

    /** Squared distance to the closest watched mob within radius, or NaN if none. */
    private double nearestWatchedMobDistanceSquared(Player player) {
        double radiusSquared = radius * radius;
        double best = Double.NaN;
        Location playerLoc = player.getLocation();

        // Only scans entities actually near the player, instead of every
        // living entity loaded in the world.
        for (org.bukkit.entity.Entity nearby : player.getWorld().getNearbyEntities(playerLoc, radius, radius, radius)) {
            if (!(nearby instanceof LivingEntity entity)) continue;
            for (String id : watchedIds) {
                if (mobs.getMob(id) != null && mobs.getMob(id).isThis(entity)) {
                    double distSquared = entity.getLocation().distanceSquared(playerLoc);
                    if (distSquared <= radiusSquared && (Double.isNaN(best) || distSquared < best)) {
                        best = distSquared;
                    }
                    break;
                }
            }
        }
        return best;
    }
}
