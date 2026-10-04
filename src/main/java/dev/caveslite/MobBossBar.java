package dev.caveslite;

import dev.caveslite.util.TagHelper;
import dev.caveslite.util.Text;
import dev.caveslite.util.Utils;
import dev.caveslite.util.WorldFilter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shows a boss bar naming whichever enabled custom mob is closest to each
 * player, so you get a warning that something is nearby without necessarily
 * seeing it.
 */
public final class MobBossBar {
    private record Nearest(String mobId, LivingEntity entity, double distanceSquared) {}

    private final WorldFilter worlds = new WorldFilter();
    private final Map<UUID, BossBar> shown = new HashMap<>();

    private boolean enabled;
    private double radius;
    private String format;
    private String progressMode;
    private BossBar.Color color;
    private BossBar.Overlay overlay;
    private Set<String> allowedIds = new HashSet<>();

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        radius = Math.max(1, cfg.getDouble("radius", 24));
        worlds.reload(cfg.getStringList("worlds"));
        format = cfg.getString("format", "&4{mob} &7cerca...");
        progressMode = cfg.getString("progress", "distance").toLowerCase(Locale.ROOT);
        color = parseEnum(BossBar.Color.class, cfg.getString("color", "RED"), BossBar.Color.RED);
        overlay = parseEnum(BossBar.Overlay.class, cfg.getString("style", "PROGRESS"), BossBar.Overlay.PROGRESS);

        allowedIds = new HashSet<>();
        ConfigurationSection mobsSection = cfg.getConfigurationSection("mobs");
        if (mobsSection != null) {
            for (String id : mobsSection.getKeys(false)) {
                if (mobsSection.getBoolean(id, true)) {
                    allowedIds.add(id.toLowerCase(Locale.ROOT));
                }
            }
        }

        if (!enabled) clearAll();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** Called periodically by the plugin's scheduler. */
    public void tick() {
        if (!enabled) return;
        for (World world : Bukkit.getWorlds()) {
            if (!worlds.allows(world)) continue;
            for (Player player : world.getPlayers()) {
                update(player);
            }
        }
    }

    private void update(Player player) {
        Nearest nearest = findNearest(player);
        if (nearest == null) {
            hide(player);
            return;
        }

        String visibleName = nearest.entity().getCustomName() != null ? nearest.entity().getCustomName() : Utils.capitalize(nearest.mobId().replace('-', ' '));
        Component name = Text.legacy(format.replace("{mob}", visibleName));
        float progress = computeProgress(nearest);

        BossBar bar = shown.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(name, progress, color, overlay);
            shown.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(name);
            bar.progress(progress);
            bar.color(color);
            bar.overlay(overlay);
        }
    }

    private float computeProgress(Nearest nearest) {
        if ("health".equals(progressMode)) {
            AttributeInstance attribute = nearest.entity().getAttribute(Attribute.MAX_HEALTH);
            double max = attribute != null ? attribute.getValue() : 0;
            if (max > 0) return clamp((float) (nearest.entity().getHealth() / max));
        }
        double distance = Math.sqrt(nearest.distanceSquared());
        return clamp((float) (1 - distance / radius));
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void hide(Player player) {
        BossBar bar = shown.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /** Call when a player disconnects, to stop tracking their bar. */
    public void onQuit(Player player) {
        shown.remove(player.getUniqueId());
    }

    private Nearest findNearest(Player player) {
        if (allowedIds.isEmpty()) return null;

        Location loc = player.getLocation();
        double radiusSquared = radius * radius;
        Nearest best = null;

        for (Entity candidate : player.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(candidate instanceof LivingEntity entity)) continue;
            String id = TagHelper.getTag(entity);
            if (id == null || !allowedIds.contains(id)) continue;

            double distanceSquared = entity.getLocation().distanceSquared(loc);
            if (distanceSquared > radiusSquared) continue;
            if (best == null || distanceSquared < best.distanceSquared()) {
                best = new Nearest(id, entity, distanceSquared);
            }
        }
        return best;
    }

    private void clearAll() {
        for (Map.Entry<UUID, BossBar> entry : shown.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        shown.clear();
    }
}
