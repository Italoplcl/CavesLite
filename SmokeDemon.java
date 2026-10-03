package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobBase;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Invisible cloud of smoke that blinds and withers everything nearby, as long as it's dark. */
public class SmokeDemon extends MobBase implements CustomMob.Ticking {
    private static final PotionEffect BLINDNESS = new PotionEffect(PotionEffectType.BLINDNESS, 120, 1);
    private static final PotionEffect WITHER = new PotionEffect(PotionEffectType.WITHER, 120, 0);
    private static final PotionEffect INVISIBILITY =
            new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false);

    private int maxLight;
    private double radius;
    private Particle particle = Particle.CLOUD;

    public SmokeDemon() {
        super(EntityType.ZOMBIE, "smoke-demon", 7, null);
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        maxLight = cfg.getInt("max-light", 11);
        radius = cfg.getInt("harm-radius", 3);
        try {
            particle = Particle.valueOf(cfg.getString("particle", "CLOUD").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            particle = Particle.CLOUD;
        }
    }

    @Override
    public boolean canSpawn(Location loc) {
        return loc.getBlock().getLightLevel() <= maxLight;
    }

    @Override
    protected void prepare(LivingEntity entity) {
        entity.setCustomNameVisible(false);
        entity.addPotionEffect(INVISIBILITY);
        entity.setSilent(true);
        entity.setCanPickupItems(false);

        EntityEquipment equipment = entity.getEquipment();
        equipment.setItemInMainHand(null);
        equipment.setItemInOffHand(null);
        equipment.setHelmet(null);
        equipment.setChestplate(null);
        equipment.setLeggings(null);
        equipment.setBoots(null);
    }

    @Override
    public void tick(LivingEntity entity) {
        if (entity.getLocation().getBlock().getLightLevel() > maxLight) {
            entity.remove();
            return;
        }
        entity.getNearbyEntities(radius, radius, radius).forEach(this::harm);
        entity.getWorld().spawnParticle(particle, entity.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0f);
    }

    private void harm(Entity entity) {
        if (entity instanceof LivingEntity living && entity.getLocation().getBlock().getLightLevel() <= maxLight) {
            living.addPotionEffect(BLINDNESS);
            living.addPotionEffect(WITHER);
        }
    }
}
