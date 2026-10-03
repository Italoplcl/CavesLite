package dev.caveslite;

import dev.caveslite.util.TagHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Leaves a faint trail of particles behind specific custom mobs. */
public final class MobTrails {
    private record TrailConfig(Particle particle, int count, double offset) {}

    private final Logger logger;
    private final Map<String, TrailConfig> trails = new HashMap<>();

    private boolean enabled;
    private double radius;

    public MobTrails(Logger logger) {
        this.logger = logger;
    }

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        radius = Math.max(1, cfg.getDouble("radius", 32));

        trails.clear();
        ConfigurationSection mobsSection = cfg.getConfigurationSection("mobs");
        if (mobsSection == null) return;

        for (String id : mobsSection.getKeys(false)) {
            ConfigurationSection mobCfg = mobsSection.getConfigurationSection(id);
            if (mobCfg == null || !mobCfg.getBoolean("enabled", false)) continue;

            String particleName = mobCfg.getString("particle", "SMOKE");
            Particle particle;
            try {
                particle = Particle.valueOf(particleName.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Unknown trail particle for {0}: {1}", new Object[]{id, particleName});
                continue;
            }
            trails.put(id.toLowerCase(Locale.ROOT), new TrailConfig(
                    particle,
                    Math.max(1, mobCfg.getInt("count", 2)),
                    Math.max(0, mobCfg.getDouble("offset", 0.3))
            ));
        }
    }

    /** Called periodically by the plugin's scheduler. */
    public void tick() {
        if (!enabled || trails.isEmpty()) return;

        Set<UUID> handled = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                Location loc = player.getLocation();
                for (Entity candidate : player.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                    if (!(candidate instanceof LivingEntity entity) || !handled.add(entity.getUniqueId())) continue;
                    String id = TagHelper.getTag(entity);
                    TrailConfig trail = id == null ? null : trails.get(id);
                    if (trail != null) {
                        play(entity, trail);
                    }
                }
            }
        }
    }

    private void play(LivingEntity entity, TrailConfig trail) {
        Location loc = entity.getLocation().add(0, entity.getHeight() / 2, 0);
        entity.getWorld().spawnParticle(trail.particle(), loc, trail.count(),
                trail.offset(), trail.offset(), trail.offset(), 0);
    }
}
