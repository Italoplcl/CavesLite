package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.Locations;
import dev.caveslite.util.Rng;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

/** Just screams and eventually dies. */
public class CryingBat extends MobBase implements CustomMob.Ticking {
    private double cryChance;
    private double deathChance;

    public CryingBat() {
        super(EntityType.BAT, "crying-bat", 9, null);
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        cryChance = cfg.getDouble("cry-chance", 3.33) / 100;
        deathChance = cfg.getDouble("death-chance", 20) / 100;
    }

    @Override
    public void tick(LivingEntity entity) {
        if (cryChance > 0 && Rng.chance(cryChance)) {
            Locations.playSound(entity.getLocation(), Sound.ENTITY_WOLF_WHINE, 1, (float) (1.4 + Rng.nextDouble(0.6)));
            if (deathChance > 0 && Rng.chance(deathChance)) {
                entity.damage(1000);
            }
        }
    }
}
