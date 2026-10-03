package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.Rng;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Spawns extra TNT when it explodes and sets off a tiny blast when it is hit. */
public class TNTCreeper extends MobBase implements Listener {
    private static final PotionEffect STRENGTH =
            new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 0, false, true);

    private int tntAmount;
    private double explosionChance;

    public TNTCreeper() {
        super(EntityType.CREEPER, "tnt-creeper", 9, null, "&4TNT Creeper");
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        tntAmount = cfg.getInt("tnt-amount", 2);
        explosionChance = cfg.getDouble("explosion-chance", 33.33) / 100;
    }

    @Override
    protected void prepare(LivingEntity entity) {
        entity.addPotionEffect(STRENGTH);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!isThis(event.getEntity())) return;
        LivingEntity creeper = (LivingEntity) event.getEntity();
        creeper.removePotionEffect(PotionEffectType.STRENGTH);
        Location loc = event.getLocation();
        for (int i = 0; i < tntAmount; i++) {
            TNTPrimed tnt = creeper.getWorld().spawn(loc, TNTPrimed.class);
            tnt.setVelocity(new Vector(Rng.nextDouble(2) - 1, 0.3, Rng.nextDouble(2) - 1));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (explosionChance > 0 && isThis(event.getEntity()) && Rng.chance(explosionChance)) {
            event.getDamager().getWorld().createExplosion(event.getDamager().getLocation(), 0.01f);
        }
    }
}
