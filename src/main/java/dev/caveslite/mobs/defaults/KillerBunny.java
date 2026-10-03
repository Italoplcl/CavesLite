package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Rabbit;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Restores Minecraft's hidden Killer Bunny as a rare cave anomaly. */
public final class KillerBunny extends MobBase {
    public KillerBunny() { super(EntityType.RABBIT, "killer-bunny", 0, 12d, "&4The Killer Bunny"); }
    @Override protected void configure(ConfigurationSection cfg) {}
    @Override protected void prepare(LivingEntity entity) {
        if (entity instanceof Rabbit rabbit) rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, true));
    }
}
