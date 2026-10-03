package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.Locations;
import dev.caveslite.util.Materials;
import dev.caveslite.util.Rng;
import org.bukkit.Material;
import org.bukkit.Sound;
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

import java.util.ArrayList;
import java.util.List;

/** Places torches when it's dark and drops loot when it gets hit. */
public class DeadMiner extends MobBase implements CustomMob.Ticking, Listener {
    private static final String DEFAULT_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzE5MzdiY2Q1YmVlYWEzNDI0NDkxM2YyNzc1MDVlMjlkMmU2ZmIzNWYyZTIzY2E0YWZhMmI2NzY4ZTM5OGQ3MyJ9fX0=";

    private boolean requiresTarget;
    private boolean torches;
    private boolean redTorches;
    private double dropChance;
    private ItemStack head;
    private List<Material> items = new ArrayList<>();
    private PotionEffect cooldownEffect;

    public DeadMiner() {
        super(EntityType.ZOMBIE, "dead-miner", 10, 22d);
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        requiresTarget = cfg.getBoolean("requires-target", true);
        torches = cfg.getBoolean("place-torches", true);
        redTorches = cfg.getBoolean("red-torches", false);
        dropChance = cfg.getDouble("drop-chance", 16.67) / 100;
        head = Materials.head(cfg.getString("head-value", DEFAULT_HEAD));
        items = new ArrayList<>(Materials.getSet(cfg.getStringList("drop-items")));

        // The cooldown between torches is tracked with a harmless nausea effect on the miner.
        int cooldownSeconds = cfg.getInt("torches-cooldown", 12);
        cooldownEffect = cooldownSeconds <= 0
                ? null
                : new PotionEffect(PotionEffectType.NAUSEA, cooldownSeconds * 20, 0, true, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (!isThis(event.getEntity()) || event.getDamage() < 1) return;
        LivingEntity miner = (LivingEntity) event.getEntity();
        if (dropChance > 0 && !items.isEmpty() && Rng.chance(dropChance)) {
            miner.getWorld().dropItemNaturally(miner.getLocation(), new ItemStack(Rng.randomElement(items)));
        }
    }

    @Override
    protected void prepare(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        equipment.setHelmet(head);
        equipment.setDropChance(EquipmentSlot.HEAD, 0f);
        equipment.setItemInMainHand(new ItemStack(Rng.nextBoolean() ? Material.IRON_PICKAXE : Material.STONE_PICKAXE));
        if (Rng.nextBoolean()) {
            equipment.setChestplate(new ItemStack(Rng.nextBoolean() ? Material.CHAINMAIL_CHESTPLATE : Material.LEATHER_CHESTPLATE));
        }
        if (Rng.nextBoolean()) {
            equipment.setBoots(new ItemStack(Rng.nextBoolean() ? Material.CHAINMAIL_BOOTS : Material.LEATHER_BOOTS));
        }
        if (torches) {
            equipment.setItemInOffHand(new ItemStack(redTorches ? Material.REDSTONE_TORCH : Material.TORCH));
        }
        entity.setCanPickupItems(false);
    }

    @Override
    public void tick(LivingEntity entity) {
        if (!torches || entity.hasPotionEffect(PotionEffectType.NAUSEA)) return;
        Block block = entity.getLocation().getBlock();

        if (block.getLightLevel() > 0) return;
        if (requiresTarget && !(entity instanceof Monster monster && monster.getTarget() != null)) return;

        if (block.getType().isAir() && Materials.isCave(block.getRelative(BlockFace.DOWN).getType())) {
            block.setType(redTorches ? Material.REDSTONE_TORCH : Material.TORCH, false);
            Locations.playSound(block.getLocation(), Sound.BLOCK_WOOD_PLACE, 1, 1);
            if (cooldownEffect != null) {
                entity.addPotionEffect(cooldownEffect);
            }
        }
    }
}
