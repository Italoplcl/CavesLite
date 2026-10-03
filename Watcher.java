package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.Locations;
import dev.caveslite.util.Materials;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Teleports behind the player the moment they look away. Just a jumpscare. */
public class Watcher extends MobBase implements CustomMob.Ticking, Listener {
    private static final String DEFAULT_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDI5MzhmMjQxZDc0NDMzZjcyZjVjMzljYjgzYThlNWZmN2UxNzdiYTdjYjQyODY5ZGI2NGUzMDc5MTAyYmZjNSJ9fX0=";

    private static final PotionEffect INVISIBILITY =
            new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false);
    private static final PotionEffect SLOWNESS = new PotionEffect(PotionEffectType.SLOWNESS, 30, 4);
    private static final PotionEffect BLINDNESS = new PotionEffect(PotionEffectType.BLINDNESS, 80, 2);
    private static final Vector ZERO_VECTOR = new Vector(0, 0, 0);

    private ItemStack head;

    public Watcher() {
        super(EntityType.HUSK, "watcher", 7, 15d);
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        head = Materials.head(cfg.getString("head-value", DEFAULT_HEAD));
    }

    @Override
    protected void prepare(LivingEntity entity) {
        entity.setSilent(true);
        entity.setCanPickupItems(false);
        entity.addPotionEffect(INVISIBILITY);

        EntityEquipment equipment = entity.getEquipment();
        equipment.setHelmet(head);
        equipment.setDropChance(EquipmentSlot.HEAD, 0f);
        equipment.setChestplate(null);
        equipment.setLeggings(null);
        equipment.setBoots(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (isThis(event.getEntity())) {
            Locations.playSound(event.getEntity().getLocation(), Sound.ENTITY_SLIME_SQUISH, 1, 1.1f);
        }
    }

    @Override
    public void tick(LivingEntity entity) {
        if (entity instanceof Monster monster && monster.getTarget() instanceof Player target) {
            if (Locations.isLookingAt(target, entity)) return;
            Location loc = target.getLocation().add(target.getLocation().getDirection());
            loc.setYaw(-loc.getYaw());
            entity.teleport(loc);
            target.setVelocity(ZERO_VECTOR);
            target.addPotionEffect(SLOWNESS);
            target.addPotionEffect(BLINDNESS);
            Locations.playSound(target.getEyeLocation(), Sound.ENTITY_GHAST_HURT, 1, 2);
        }
    }
}
