package dev.caveslite;

import dev.caveslite.util.TagHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Common optional extra drops and console command for every CavesLite mob. */
public final class MobDeathActions implements Listener {
    private final CavesLite plugin;
    public MobDeathActions(CavesLite plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String id = TagHelper.getTag(entity);
        if (id == null) return;
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("mobs." + id);
        if (cfg == null) return;

        for (String raw : cfg.getStringList("extra-drops")) {
            Material material = Material.matchMaterial(raw);
            if (material != null && material.isItem()) event.getDrops().add(new ItemStack(material));
        }

        ConfigurationSection command = cfg.getConfigurationSection("death-command");
        if (command == null || !command.getBoolean("enabled", false)) return;
        String raw = command.getString("command", "");
        Player killer = entity.getKiller();
        if (raw == null || raw.isBlank() || killer == null) return;
        var loc = entity.getLocation();
        String parsed = raw
                .replace("%player%", killer.getName()).replace("{player}", killer.getName())
                .replace("%mob%", id).replace("%world%", loc.getWorld().getName())
                .replace("%x%", Integer.toString(loc.getBlockX()))
                .replace("%y%", Integer.toString(loc.getBlockY()))
                .replace("%z%", Integer.toString(loc.getBlockZ()));
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed));
    }
}
