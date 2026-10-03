package dev.caveslite.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Utils {
    private Utils() {}

    /** Returns the section, or an empty one so callers never have to null-check. */
    public static ConfigurationSection section(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent == null ? null : parent.getConfigurationSection(path);
        return section != null ? section : new MemoryConfiguration();
    }

    public static void setMaxHealth(LivingEntity entity, double health) {
        AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(health);
        }
        entity.setHealth(health);
    }

    public static double getDouble(String str, double def) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static String capitalize(String str) {
        StringBuilder builder = new StringBuilder(str.length());
        for (String word : str.split(" ")) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    /** Entity types by config name (ZOMBIE, zombie, minecraft:zombie...). Unknown names are skipped. */
    public static Set<EntityType> entityTypes(Collection<String> names) {
        Set<EntityType> types = new HashSet<>();
        for (String name : names) {
            String lower = name.trim().toLowerCase(Locale.ROOT);
            NamespacedKey key = lower.contains(":") ? NamespacedKey.fromString(lower) : NamespacedKey.minecraft(lower);
            EntityType type = key == null ? null : Registry.ENTITY_TYPE.get(key);
            if (type != null) types.add(type);
        }
        return types;
    }
}
