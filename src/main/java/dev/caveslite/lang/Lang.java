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

    public Lang(Plugin plugin) {
        this.plugin = plugin;
    }

    /** (Re)loads the language named by "language" in config.yml (default "es"). Falls back to es.yml for any missing key. */
    public void reload(String languageCode) {
        this.code = (languageCode == null || languageCode.isBlank()) ? "es" : languageCode.toLowerCase(Locale.ROOT);
        fallback = loadOrExtract("es");
        active = "es".equals(code) ? fallback : loadOrExtract(code);
    }

    private YamlConfiguration loadOrExtract(String languageCode) {
        File file = new File(plugin.getDataFolder(), "lang/" + languageCode + ".yml");
        if (!file.exists()) {
            String resourcePath = "lang/" + languageCode + ".yml";
            if (plugin.getResource(resourcePath) != null) {
                plugin.saveResource(resourcePath, false);
            } else {
                // Unknown language code - warn once and fall back to the bundled Spanish file.
                plugin.getLogger().log(Level.WARNING,
                        "No bundled or custom lang file for \"{0}\" - using es.yml instead.", languageCode);
                return fallback != null ? fallback : loadOrExtract("es");
            }
        }
        return YamlConfiguration.loadConfiguration(file);
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
