package dev.caveslite.mobs;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public interface CustomMob {

    /** Lower-case-with-hyphens id, also the config section name under "mobs". */
    String id();

    /** Explicit natural-spawn switch. Manual summon remains available while disabled. */
    default boolean enabled() { return true; }

    /** Weight in the natural spawn pool. */
    int weight();

    /** Optional common natural-spawn limits. */
    default int spawnYMin() { return Integer.MIN_VALUE; }
    default int spawnYMax() { return Integer.MAX_VALUE; }
    default int maxActive() { return 0; }
    default long cooldownMillis() { return 0L; }

    /** Vanilla entity this mob is based on. */
    EntityType type();

    boolean isThis(Entity entity);

    default boolean canSpawn(Location loc) {
        return true;
    }

    /** Whether this mob uses the shared global mobs.y-min/y-max range. */
    default boolean usesGlobalYRange() { return true; }

    /** Global natural-spawn context. Standard mobs default to caves; anomalies may opt into another context. */
    default boolean naturalContextAllowed(Location loc) {
        return dev.caveslite.util.Locations.isCave(loc);
    }

    LivingEntity spawn(Location loc);

    void reload(ConfigurationSection cfg);

    /** A mob that needs to be ticked every few ticks while it's alive and loaded. */
    interface Ticking extends CustomMob {
        void tick(LivingEntity entity);
    }

    /** A mob that can leave temporary world artifacts that must be cleaned explicitly. */
    interface Cleanup extends CustomMob {
        int cleanupArtifacts();
    }
}
