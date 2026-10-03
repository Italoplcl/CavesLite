package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.MobBase;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

/** Makes Minecraft's normally-unused Illusioner available as a rare encounter. */
public final class IllusionerAnomaly extends MobBase {
    public IllusionerAnomaly() { super(EntityType.ILLUSIONER, "illusioner", 0, 32d, "&5Illusioner"); }
    @Override protected void configure(ConfigurationSection cfg) {}
}
