package dev.caveslite.mobs;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public interface CustomMob {

    /** Lower-case-with-hyphens id, also the config section name under "mobs". */
    String id();

    /** Weight in the spawn pool. 0 disables natural spawning. */
    int weight();

    /** Vanilla entity this mob is based on. */
    EntityType type();

    boolean isThis(Entity entity);

    default boolean canSpawn(Location loc) {
        return true;
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
