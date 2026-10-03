package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.Materials;
import dev.caveslite.util.Rng;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Burning zombie that leaves a trail of fire and magma blocks behind it. */
public class MagmaMonster extends MobBase implements CustomMob.Ticking, Listener {
    private static final PotionEffect FIRE_RESISTANCE =
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 1, false, false);
    private static final PotionEffect INVISIBILITY =
            new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 1, false, false);

    private double fireChance;
    private double magmaChance;
    private boolean extinguishedDamage;
    private boolean requiresTarget;

    public MagmaMonster() {
        super(EntityType.ZOMBIE, "magma-monster", 4, null);
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        fireChance = cfg.getDouble("fire-chance", 7.14) / 100;
        magmaChance = cfg.getDouble("magma-chance", 3.57) / 100;
        extinguishedDamage = cfg.getBoolean("extinguished-damage", false);
        requiresTarget = cfg.getBoolean("requires-target", true);
    }

    @Override
    protected void prepare(LivingEntity entity) {
        entity.setSilent(true);
        entity.setCanPickupItems(false);

        EntityEquipment equipment = entity.getEquipment();
        equipment.setItemInMainHand(new ItemStack(Material.BLAZE_POWDER));
        equipment.setItemInOffHand(new ItemStack(Material.BLAZE_POWDER));
        equipment.setHelmet(null);
        equipment.setChestplate(Materials.coloredLeather(Material.LEATHER_CHESTPLATE, 115, 57, 34));
        equipment.setLeggings(Materials.coloredLeather(Material.LEATHER_LEGGINGS, 115, 57, 34));
        equipment.setBoots(Materials.coloredLeather(Material.LEATHER_BOOTS, 115, 57, 34));
        equipment.setDropChance(EquipmentSlot.CHEST, 0f);
        equipment.setDropChance(EquipmentSlot.LEGS, 0f);
        equipment.setDropChance(EquipmentSlot.FEET, 0f);

        entity.addPotionEffect(FIRE_RESISTANCE);
        entity.addPotionEffect(INVISIBILITY);
        entity.setFireTicks(20);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (isThis(event.getDamager()) && Rng.nextBoolean()) {
            event.getEntity().setFireTicks(60);
        }
    }

    @Override
    public void tick(LivingEntity entity) {
        // Keep it burning; if it got extinguished it can optionally take a bit of damage.
        if (extinguishedDamage && entity.getFireTicks() > 0) {
            entity.damage(0.1);
        } else {
            entity.setFireTicks(20);
        }

        if (requiresTarget && !(entity instanceof Monster monster && monster.getTarget() != null)) return;

        if (fireChance > 0 && Rng.chance(fireChance)) {
            Block block = entity.getLocation().getBlock();
            if (block.getType().isAir() && !block.getRelative(BlockFace.DOWN).isPassable()) {
                block.setType(Material.FIRE, false);
            }
        }

        if (magmaChance > 0 && Rng.chance(magmaChance)) {
            Block below = entity.getLocation().subtract(0, 1, 0).getBlock();
            if (below.getType() != Material.BEDROCK && Materials.isCave(below.getType())) {
                below.setType(Material.MAGMA_BLOCK, false);
            }
        }
    }
}
