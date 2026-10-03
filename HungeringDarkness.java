package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.Locations;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Spawns in complete darkness and kills players who aren't standing in light. */
public class HungeringDarkness extends MobBase implements Listener {
    private static final PotionEffect INVISIBILITY =
            new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false);
    private static final PotionEffect SLOWNESS =
            new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 3, false, false);

    private double damage;
    private boolean removeOnLight;
    private boolean nightVisionCounts;
    private boolean deathSound;

    public HungeringDarkness() {
        super(EntityType.VEX, "hungering-darkness", 8, null);
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        damage = cfg.getDouble("damage", 200);
        removeOnLight = cfg.getBoolean("remove-on-light", false);
        nightVisionCounts = cfg.getBoolean("night-vision", false);
        deathSound = cfg.getBoolean("death-sound", true);
    }

    @Override
    public boolean canSpawn(Location location) {
        return location.getBlock().getLightLevel() == 0;
    }

    @Override
    protected void prepare(LivingEntity entity) {
        entity.setCustomNameVisible(false);
        entity.setInvulnerable(true);
        entity.setCollidable(false);
        entity.addPotionEffect(INVISIBILITY);
        entity.addPotionEffect(SLOWNESS);
    }

    /** True if the target is protected by light (and it is not a night-vision "cheat"). */
    private boolean isLit(LivingEntity target) {
        return target.getLocation().getBlock().getLightLevel() > 0
                && (!nightVisionCounts || target.hasPotionEffect(PotionEffectType.NIGHT_VISION));
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        Entity target = event.getTarget();
        if (!isThis(event.getEntity()) || target == null) return;
        if (target.getLocation().getBlock().getLightLevel() > 0
                && (!nightVisionCounts || !(target instanceof LivingEntity living)
                || living.hasPotionEffect(PotionEffectType.NIGHT_VISION))) {
            event.setCancelled(true);
            die(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getDamager() instanceof LivingEntity attacker)
                || !isThis(attacker)) {
            return;
        }
        if (isLit(player)) {
            event.setCancelled(true);
            die(attacker);
        } else {
            event.setDamage(damage);
        }
    }

    private void die(Entity entity) {
        if (!removeOnLight) return;
        if (deathSound) {
            Locations.playSound(entity.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1f, 0.8f);
        }
        entity.remove();
    }
}
