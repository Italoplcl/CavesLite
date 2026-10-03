package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.TagHelper;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;

/** Classic spider jockey promoted to an intentional rare encounter. */
public final class SpiderJockeyAnomaly extends MobBase {
    public SpiderJockeyAnomaly() { super(EntityType.SPIDER, "spider-jockey", 0, 20d, "&4Spider Jockey"); }
    @Override protected void configure(ConfigurationSection cfg) {}
    @Override public LivingEntity spawn(Location loc) {
        Spider spider = (Spider) loc.getWorld().spawnEntity(loc, EntityType.SPIDER);
        Skeleton rider = (Skeleton) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
        TagHelper.setTag(spider, id()); TagHelper.setTag(rider, id());
        spider.addPassenger(rider);
        return spider;
    }
}
