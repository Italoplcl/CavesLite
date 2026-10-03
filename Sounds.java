package dev.caveslite.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

/**
 * Sound is no longer an enum, so "MUSIC_DISC_11"-style names from the config
 * are resolved through the registry. Also accepts "music_disc.11" or
 * "minecraft:music_disc.11".
 */
public final class Sounds {
    private Sounds() {}

    public static Sound find(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return null;

        NamespacedKey direct = NamespacedKey.fromString(trimmed.toLowerCase(Locale.ROOT));
        if (direct != null) {
            Sound sound = Registry.SOUNDS.get(direct);
            if (sound != null) return sound;
        }

        String wanted = trimmed.toUpperCase(Locale.ROOT).replace('.', '_');
        for (Sound sound : Registry.SOUNDS) {
            NamespacedKey key = Registry.SOUNDS.getKey(sound);
            if (key != null && key.getKey().toUpperCase(Locale.ROOT).replace('.', '_').equals(wanted)) {
                return sound;
            }
        }
        return null;
    }
}
