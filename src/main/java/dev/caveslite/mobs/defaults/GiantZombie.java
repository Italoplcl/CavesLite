package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Rare overworld night encounter whose identity is simply being enormous and powerful. */
public final class GiantZombie extends MobBase {
    private int minY;
    private int maxY;

    public GiantZombie() { super(EntityType.GIANT, "giant-zombie", 0, 80d, "&4Giant Zombie"); }

    @Override protected void configure(ConfigurationSection cfg) {
        minY = cfg.getInt("spawn-y-min", 80);
        maxY = cfg.getInt("spawn-y-max", 320);
    }

    @Override public boolean usesGlobalYRange() { return false; }

    @Override public boolean naturalContextAllowed(Location loc) {
        World w = loc.getWorld();
        long t = w.getTime();
        return w.getEnvironment() == World.Environment.NORMAL && t >= 13000 && t <= 23000
                && loc.getBlockY() >= minY && loc.getBlockY() <= maxY && loc.getBlock().getLightFromSky() > 0;
    }

    @Override protected void prepare(LivingEntity entity) {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 0, false, true));
    }
}
