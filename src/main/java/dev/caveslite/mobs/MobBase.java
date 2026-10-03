package dev.caveslite.mobs;

import dev.caveslite.util.TagHelper;
import dev.caveslite.util.Text;
import dev.caveslite.util.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;

public abstract class MobBase implements CustomMob {
    private final EntityType type;
    private final String id;
    private final String scoreboardTag;
    private final int defWeight;
    private final Double defHealth;
    private final String defName;

    private int weight;
    protected Component name;
    protected Double health;
    private int spawnYMin = Integer.MIN_VALUE;
    private int spawnYMax = Integer.MAX_VALUE;
    private int maxActive;
    private long cooldownMillis;

    protected MobBase(EntityType type, String id, int weight, Double health) {
        this(type, id, weight, health, "&4" + Utils.capitalize(id.replace('-', ' ')));
    }

    protected MobBase(EntityType type, String id, int weight, Double health, String name) {
        this.type = type;
        this.id = id.toLowerCase(Locale.ROOT);
        this.scoreboardTag = TagHelper.mobScoreboardTag(this.id);
        this.defWeight = weight;
        this.defHealth = health;
        this.defName = name;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int weight() {
        return weight;
    }

    @Override public int spawnYMin() { return spawnYMin; }
    @Override public int spawnYMax() { return spawnYMax; }
    @Override public int maxActive() { return maxActive; }
    @Override public long cooldownMillis() { return cooldownMillis; }

    @Override
    public EntityType type() {
        return type;
    }

    @Override
    public boolean isThis(Entity entity) {
        return entity instanceof LivingEntity && entity.getScoreboardTags().contains(scoreboardTag);
    }

    @Override
    public void reload(ConfigurationSection cfg) {
        weight = cfg.getInt("priority", defWeight);
        spawnYMin = cfg.getInt("spawn-y-min", Integer.MIN_VALUE);
        spawnYMax = cfg.getInt("spawn-y-max", Integer.MAX_VALUE);
        maxActive = Math.max(0, cfg.getInt("max-active", 0));
        cooldownMillis = Math.max(0L, cfg.getLong("cooldown-seconds", 0L)) * 1000L;

        String configuredName = cfg.getString("name", defName);
        name = configuredName == null || configuredName.isEmpty() ? null : Text.legacy(configuredName);

        // YAML "health: 20" is an Integer, so accept any number.
        health = cfg.get("health") instanceof Number number
                ? Double.valueOf(Math.max(number.doubleValue(), 1))
                : defHealth;

        configure(cfg);
    }

    protected abstract void configure(ConfigurationSection cfg);

    /** Equipment, potion effects and so on. Called right after spawning. */
    protected void prepare(LivingEntity entity) {
    }

    @Override
    public LivingEntity spawn(Location loc) {
        LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        TagHelper.setTag(entity, id);
        entity.customName(name);
        if (health != null) {
            Utils.setMaxHealth(entity, health);
        }
        prepare(entity);
        return entity;
    }
}
