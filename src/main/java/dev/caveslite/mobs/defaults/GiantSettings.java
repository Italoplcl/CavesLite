package dev.caveslite.mobs.defaults;

import org.bukkit.configuration.ConfigurationSection;

/** Validated knobs shared by giant behavior and verification. Damage is base damage. */
public record GiantSettings(boolean chase, double speed, double followRange, int repathTicks,
                            Attack melee, Attack stomp, Attack quake, double waveStep,
                            boolean reinforcements, int amount, int maxMinions, int maxWaves,
                            int summonCooldown, int minionLifetime, double minionHealth,
                            String warningSound, String impactSound, float volume, float pitch) {
    public record Attack(boolean enabled, double damage, double radius, double knockback,
                         int cooldownTicks, int warningTicks) {}

    public static double bounded(double value, double fallback, double min, double max) {
        return Math.max(min, Math.min(max, Double.isFinite(value) ? value : fallback));
    }

    private static double number(ConfigurationSection cfg, String key, double fallback, double min, double max) {
        return bounded(cfg.getDouble(key, fallback), fallback, min, max);
    }

    private static Attack attack(ConfigurationSection cfg, String key, double damage, double radius,
                                 double knockback, int cooldown, int warning) {
        return new Attack(cfg.getBoolean(key + ".enabled", true),
                number(cfg,key+".damage",damage,0,100), number(cfg,key+".radius",radius,0.5,16),
                number(cfg,key+".knockback",knockback,0,2),
                (int) number(cfg,key+".cooldown-ticks",cooldown,8,72000),
                (int) number(cfg,key+".warning-ticks",warning,0,200));
    }

    public static GiantSettings load(ConfigurationSection cfg) {
        return new GiantSettings(cfg.getBoolean("movement.enabled",true),
                number(cfg,"movement.speed",0.23,0.01,0.5), number(cfg,"movement.follow-range",32,4,64),
                (int)number(cfg,"movement.repath-ticks",20,8,200),
                attack(cfg,"attacks.basic",6,3,0.45,30,0),
                attack(cfg,"attacks.stomp",8,4.5,0.7,200,24),
                attack(cfg,"attacks.quake",6,8,0.6,360,32),
                number(cfg,"attacks.quake.wave-step",1,0.25,2),
                cfg.getBoolean("reinforcements.enabled",true),
                (int)number(cfg,"reinforcements.amount",2,1,8),
                (int)number(cfg,"reinforcements.max-active",3,1,12),
                (int)number(cfg,"reinforcements.max-waves",2,1,10),
                (int)number(cfg,"reinforcements.cooldown-seconds",30,5,3600)*20,
                (int)number(cfg,"reinforcements.lifetime-seconds",45,5,600)*20,
                number(cfg,"reinforcements.health",16,1,100),
                cfg.getString("sounds.warning","minecraft:entity.zombie.ambient"),
                cfg.getString("sounds.impact","minecraft:entity.generic.explode"),
                (float)number(cfg,"sounds.volume",1,0,4), (float)number(cfg,"sounds.pitch",0.6,0.1,2));
    }
}
