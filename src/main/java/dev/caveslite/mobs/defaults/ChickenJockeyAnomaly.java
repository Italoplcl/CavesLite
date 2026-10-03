package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.TagHelper;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** A deliberately absurdly fast chicken jockey. */
public final class ChickenJockeyAnomaly extends MobBase {
    private int speedAmplifier;
    public ChickenJockeyAnomaly() { super(EntityType.CHICKEN, "chicken-jockey", 0, 16d, "&cChicken Jockey"); }
    @Override protected void configure(ConfigurationSection cfg) { speedAmplifier = Math.max(0, cfg.getInt("speed-amplifier", 3)); }
    @Override public LivingEntity spawn(Location loc) {
        Chicken chicken = (Chicken) loc.getWorld().spawnEntity(loc, EntityType.CHICKEN);
        Zombie rider = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        rider.setBaby();
        TagHelper.setTag(chicken, id());
        TagHelper.setTag(rider, id());
        chicken.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, speedAmplifier, false, true));
        chicken.addPassenger(rider);
        return chicken;
    }
}
