package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.Locations;
import dev.caveslite.util.Rng;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Spawns cave spiders as minions and covers the victim in cobwebs. */
public class AlphaSpider extends MobBase implements Listener {
    private static final PotionEffect POISON = new PotionEffect(PotionEffectType.POISON, 75, 1);
    private static final PotionEffect REGENERATION =
            new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 0, false, true);

    private double cobwebChance;
    private double minionChance;

    public AlphaSpider() {
        super(EntityType.SPIDER, "alpha-spider", 9, 18d);
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        cobwebChance = cfg.getDouble("cobweb-chance", 14.29) / 100;
        minionChance = cfg.getDouble("minion-chance", 6.67) / 100;
    }

    @Override
    protected void prepare(LivingEntity entity) {
        entity.addPotionEffect(REGENERATION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (isThis(event.getEntity()) && event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!isThis(event.getDamager()) || !(event.getEntity() instanceof LivingEntity victim)) return;
        Entity spider = event.getDamager();

        if (Rng.nextBoolean()) {
            if (minionChance > 0 && Rng.chance(minionChance)) {
                spider.getWorld().spawnEntity(spider.getLocation(), EntityType.CAVE_SPIDER);
            }
            if (cobwebChance > 0) {
                Location loc = victim.getLocation();
                loc.getBlock().setType(Material.COBWEB);
                victim.getEyeLocation().getBlock().setType(Material.COBWEB);

                Locations.loop(3, loc, l -> {
                    if (l.getBlock().getType().isAir() && Rng.chance(cobwebChance)) {
                        l.getBlock().setType(Material.COBWEB);
                    }
                });
            }
        } else {
            victim.addPotionEffect(POISON);
        }
    }
}
