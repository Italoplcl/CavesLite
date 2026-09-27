package dev.caveslite;

import dev.caveslite.mobs.MobManager;
import dev.caveslite.util.TagHelper;
import dev.caveslite.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Shows a short Action Bar warning when an enabled custom mob is close to a player. */
public final class MobWarnings {
    private boolean enabled;
    private double radius;
    private Component message;
    private Set<String> allowedIds = new HashSet<>();

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        radius = Math.max(1, cfg.getDouble("radius", 10));
        message = Text.legacy(cfg.getString("message", "&7Sientes que algo te observa..."));

        allowedIds = new HashSet<>();
        ConfigurationSection mobsSection = cfg.getConfigurationSection("mobs");
        if (mobsSection != null) {
            for (String id : mobsSection.getKeys(false)) {
                if (mobsSection.getBoolean(id, true)) {
                    allowedIds.add(id.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    /** Called periodically by the plugin's scheduler. */
    public void tick() {
        if (!enabled || allowedIds.isEmpty()) return;

        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                if (isMobNearby(player)) {
                    player.sendActionBar(message);
                }
            }
        }
    }

    private boolean isMobNearby(Player player) {
        Location loc = player.getLocation();
        for (Entity candidate : player.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(candidate instanceof LivingEntity entity)) continue;
            String id = TagHelper.getTag(entity);
            if (id != null && allowedIds.contains(id)) {
                return true;
            }
        }
        return false;
    }
}
