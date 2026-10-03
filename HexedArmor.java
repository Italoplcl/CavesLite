package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.Materials;
import dev.caveslite.util.Rng;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/** An invisible zombie wearing cursed armor; when it hits, it swaps its armor with yours. */
public class HexedArmor extends MobBase implements Listener {
    private static final PotionEffect INVISIBILITY =
            new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 1, false, false);

    private double chance;
    private boolean binding;

    public HexedArmor() {
        super(EntityType.ZOMBIE, "hexed-armor", 6, null);
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        binding = cfg.getBoolean("binding-curse", true);
        chance = cfg.getDouble("apply-chance", 25) / 100;
    }

    @Override
    protected void prepare(LivingEntity entity) {
        entity.addPotionEffect(INVISIBILITY);
        entity.setSilent(true);
        entity.setCanPickupItems(false);

        EntityEquipment equipment = entity.getEquipment();
        equipment.setHelmet(randomPiece(Materials.HELMETS));
        equipment.setChestplate(randomPiece(Materials.CHESTPLATES));
        equipment.setLeggings(randomPiece(Materials.LEGGINGS));
        equipment.setBoots(randomPiece(Materials.BOOTS));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getDamager() instanceof LivingEntity attacker)
                || !isThis(attacker)
                || !Rng.chance(chance)) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) {
                attacker.getWorld().dropItemNaturally(attacker.getLocation(), item);
            }
        }
        inventory.setArmorContents(attacker.getEquipment().getArmorContents());
        attacker.getEquipment().clear();
        attacker.remove();
    }

    private ItemStack randomPiece(List<Material> options) {
        return curse(new ItemStack(Rng.randomElement(options)));
    }

    private ItemStack curse(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (binding) meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}
