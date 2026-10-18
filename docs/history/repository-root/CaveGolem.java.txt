package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import dev.caveslite.mobs.MobManager;
import dev.caveslite.util.Locations;
import dev.caveslite.util.Materials;
import dev.caveslite.util.Rng;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Hits hard, only takes real damage from pickaxes, and can burst out of ore blocks. */
public class CaveGolem extends MobBase implements Listener {
    private static final PotionEffect SLOWNESS =
            new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 0);
    private static final PotionEffect BLINDNESS = new PotionEffect(PotionEffectType.BLINDNESS, 30, 0);
    private static final PotionEffect NAUSEA = new PotionEffect(PotionEffectType.NAUSEA, 20, 0);
    private static final PotionEffect SLOWNESS_VICTIM = new PotionEffect(PotionEffectType.SLOWNESS, 40, 1);

    private final MobManager manager;
    private final List<ItemStack> heads = new ArrayList<>();
    private Set<Material> variants = Set.of();

    private boolean slow;
    private boolean distract;
    private double nonPickaxeModifier;
    private double pickaxeModifier;
    private double damageModifier;
    private double spawnFromBlockChance;

    public CaveGolem(MobManager manager) {
        super(EntityType.SKELETON, "cave-golem", 3, 35d);
        this.manager = manager;
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        slow = cfg.getBoolean("slowness", true);
        distract = cfg.getBoolean("distract-attack", true);
        nonPickaxeModifier = cfg.getDouble("nonpickaxe-modifier", 0.07);
        pickaxeModifier = cfg.getDouble("pickaxe-modifier", 2.0);
        damageModifier = cfg.getDouble("damage-modifier", 2.0);
        spawnFromBlockChance = cfg.getDouble("spawn-from-block", 0) / 100;

        variants = Materials.getSet(cfg.getStringList("variants"));
        heads.clear();
        variants.forEach(m -> heads.add(new ItemStack(m)));
        if (heads.isEmpty()) heads.add(new ItemStack(Material.STONE));
    }

    @Override
    protected void prepare(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        equipment.setItemInMainHand(null);
        equipment.setHelmet(Rng.randomElement(heads));
        equipment.setChestplate(Materials.coloredLeather(Material.LEATHER_CHESTPLATE, 105, 105, 105));
        equipment.setLeggings(Materials.coloredLeather(Material.LEATHER_LEGGINGS, 105, 105, 105));
        equipment.setBoots(Materials.coloredLeather(Material.LEATHER_BOOTS, 105, 105, 105));
        equipment.setDropChance(EquipmentSlot.HEAD, 1f);
        equipment.setDropChance(EquipmentSlot.CHEST, 0f);
        equipment.setDropChance(EquipmentSlot.LEGS, 0f);
        equipment.setDropChance(EquipmentSlot.FEET, 0f);
        entity.setSilent(true);
        if (slow) entity.addPotionEffect(SLOWNESS);
    }

    /** Small chance that breaking one of the "variants" ores releases a golem. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (spawnFromBlockChance <= 0) return;
        Block block = event.getBlock();
        if (manager.isWorldEnabled(block.getWorld())
                && variants.contains(block.getType())
                && Rng.chance(spawnFromBlockChance)) {
            Material ore = block.getType();
            event.setDropItems(false);
            event.setExpToDrop(0);
            Location loc = block.getLocation().add(0.5, 0, 0.5);
            manager.spawn(this, loc).getEquipment().setHelmet(new ItemStack(ore));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!isThis(event.getDamager()) || !(event.getEntity() instanceof LivingEntity victim)) return;
        Locations.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2, 0.5f);
        if (distract) {
            victim.addPotionEffect(BLINDNESS);
            victim.addPotionEffect(SLOWNESS_VICTIM);
            victim.addPotionEffect(NAUSEA);
        }
        event.setDamage(event.getDamage() * damageModifier);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!isThis(entity)) return;

        if (event instanceof EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager() instanceof Player player) {
                if (Tag.ITEMS_PICKAXES.isTagged(player.getInventory().getItemInMainHand().getType())) {
                    Locations.playSound(entity.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 0.6f, 1);
                    event.setDamage(event.getDamage() * pickaxeModifier);
                    return;
                }
                Locations.playSound(entity.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 1, 1);
            }
        } else {
            Locations.playSound(entity.getLocation(), Sound.BLOCK_STONE_BREAK, SoundCategory.HOSTILE, 2, 0.6f);
        }
        event.setDamage(event.getDamage() * nonPickaxeModifier);
    }
}
