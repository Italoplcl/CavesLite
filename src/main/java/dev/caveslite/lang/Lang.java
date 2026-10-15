package dev.caveslite.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads a language file (lang/&lt;code&gt;.yml) and resolves message keys with
 * {placeholder} substitution. Any plugin-wide message - command feedback,
 * Herobrine's lines, anything added later - should go through here instead
 * of being a literal string in Java, so the whole plugin stays translatable
 * from one place.
 *
 * Bundled languages ship inside the jar (resources/lang/*.yml) and are
 * copied to plugins/CavesLite/lang/ on first run, same as config.yml,
 * so server owners can edit or add their own without recompiling.
 */
public final class Lang {
    private final Plugin plugin;
    private YamlConfiguration active;
    private YamlConfiguration fallback;
    private String code = "es";
    private YamlConfiguration legacyDisplay;

    public Lang(Plugin plugin) {
        this.plugin = plugin;
    }

    /** User file -> bundled selected language -> user Spanish -> bundled Spanish/English. */
    public void reload(String languageCode) {
        code = languageCode == null ? "es" : languageCode.trim().toLowerCase(Locale.ROOT);
        if (!code.matches("[a-z0-9_-]+")) code = "es";
        try (InputStream stream = plugin.getResource("legacy-display.yml")) {
            legacyDisplay = stream == null ? new YamlConfiguration() : YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (java.io.IOException ex) { legacyDisplay = new YamlConfiguration(); }
        YamlConfiguration english = bundled("en");
        YamlConfiguration spanish = bundled("es");
        spanish.setDefaults(english);
        fallback = loadOrExtract("es");
        fallback.setDefaults(spanish);
        YamlConfiguration selected = bundled(code);
        selected.setDefaults(fallback);
        active = loadOrExtract(code);
        active.setDefaults(selected);
        // Export both supported languages without overwriting administrator edits.
        loadOrExtract("en");
    }

    private YamlConfiguration bundled(String languageCode) {
        try (InputStream stream = plugin.getResource("lang/" + languageCode + ".yml")) {
            return stream == null ? new YamlConfiguration() : YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (java.io.IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not load bundled language", ex);
            return new YamlConfiguration();
        }
    }

    private YamlConfiguration loadOrExtract(String languageCode) {
        File file = new File(plugin.getDataFolder(), "lang/" + languageCode + ".yml");
        if (!file.exists()) {
            try (InputStream stream = plugin.getResource("lang/" + languageCode + ".yml")) {
                if (stream != null) plugin.saveResource("lang/" + languageCode + ".yml", false);
                else return new YamlConfiguration();
            } catch (java.io.IOException ex) {
                plugin.getLogger().log(Level.WARNING, "Could not extract language", ex);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public String resolve(String value) {
        return value != null && value.startsWith("@lang:") ? get(value.substring(6)) : value;
    }

    public String displayValue(String namespace, String key, String value) {
        String langKey = namespace + "." + key;
        return value != null && value.equals(legacyDisplay.getString(langKey)) && active.isString(langKey)
                ? get(langKey) : resolve(value);
    }

    /** A detached display copy keeps saved config values and language references intact. */
    public YamlConfiguration displayCopy(org.bukkit.configuration.ConfigurationSection source, String namespace) {
        YamlConfiguration copy = new YamlConfiguration();
        for (String key : source.getKeys(true)) {
            Object value = source.get(key);
            if (!(value instanceof org.bukkit.configuration.ConfigurationSection)) {
                if (value instanceof String text) {
                    copy.set(key, displayValue(namespace, key, text));
                } else copy.set(key, value);
            }
        }
        return copy;
    }

    /** Raw message (with & codes, ready for Text.legacy()), {key} placeholders replaced. Missing key returns the key itself, visibly, so it's easy to spot. */
    public String get(String key, Map<String, String> placeholders) {
        String raw = active.getString(key);
        if (raw == null) {
            raw = fallback.getString(key);
        }
        if (raw == null) {
            return "&c[missing lang key: " + key + "]";
        }
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return raw;
    }

    public String get(String key) {
        return get(key, null);
    }

    public String code() {
        return code;
    }
}
