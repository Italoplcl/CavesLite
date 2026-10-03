package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.TagHelper;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SkeletonHorse;

/** A controllable rare version of Minecraft's skeleton horseman encounter. */
public final class SkeletonHorsemanAnomaly extends MobBase {
    public SkeletonHorsemanAnomaly() { super(EntityType.SKELETON_HORSE, "skeleton-horseman", 0, 30d, "&7Skeleton Horseman"); }
    @Override protected void configure(ConfigurationSection cfg) {}
    @Override public LivingEntity spawn(Location loc) {
        SkeletonHorse horse = (SkeletonHorse) loc.getWorld().spawnEntity(loc, EntityType.SKELETON_HORSE);
        Skeleton rider = (Skeleton) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
        TagHelper.setTag(horse, id()); TagHelper.setTag(rider, id());
        horse.addPassenger(rider);
        return horse;
    }
}
