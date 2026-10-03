package dev.caveslite;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobManager;
import dev.caveslite.util.Text;
import dev.caveslite.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Flashes a dramatic title/subtitle to nearby players when a custom mob spawns. */
public final class MobSpawnTitles implements MobManager.SpawnListener {
    private final Map<UUID, Long> lastShown = new HashMap<>();

    private boolean enabled;
    private double radius;
    private String titleText;
    private String subtitleText;
    private Title.Times times;
    private long cooldownTicks;
    private Set<String> allowedIds = new HashSet<>();

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        radius = Math.max(1, cfg.getDouble("radius", 20));
        titleText = cfg.getString("title", "&4\u00a1Cuidado!");
        subtitleText = cfg.getString("subtitle", "&7{mob} ha aparecido cerca");
        cooldownTicks = Math.max(0, cfg.getLong("cooldown-ticks", 100));

        times = Title.Times.times(
                ticksToDuration(cfg.getInt("fade-in-ticks", 10)),
                ticksToDuration(cfg.getInt("stay-ticks", 40)),
                ticksToDuration(cfg.getInt("fade-out-ticks", 10))
        );

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

    private Duration ticksToDuration(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    @Override
    public void onSpawn(CustomMob mob, LivingEntity entity) {
        if (!enabled || !allowedIds.contains(mob.id())) return;

        Location loc = entity.getLocation();
        double radiusSquared = radius * radius;
        long now = entity.getWorld().getFullTime();

        Component title = Text.legacy(titleText);
        Component subtitle = Text.legacy(subtitleText.replace("{mob}", Utils.capitalize(mob.id().replace('-', ' '))));
        Title displayed = Title.title(title, subtitle, times);

        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) > radiusSquared) continue;

            Long last = lastShown.get(player.getUniqueId());
            if (last != null && now - last < cooldownTicks) continue;

            player.showTitle(displayed);
            lastShown.put(player.getUniqueId(), now);
        }
    }
}
