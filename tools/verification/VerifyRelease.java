import dev.caveslite.lang.Lang;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.nio.file.*;
import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.regex.*;
import java.util.logging.Logger;

public class VerifyRelease {
    static int checks;
    static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]), resources = root.resolve("src/main/resources");
        Path data = root.resolve("build/verification-data"); Files.createDirectories(data);
        Plugin plugin = (Plugin)Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class[]{Plugin.class}, (o,m,a) -> switch(m.getName()) {
            case "getDataFolder" -> data.toFile();
            case "getResource" -> Files.exists(resources.resolve((String)a[0])) ? Files.newInputStream(resources.resolve((String)a[0])) : null;
            case "saveResource" -> { Path target=data.resolve((String)a[0]); Files.createDirectories(target.getParent()); Files.copy(resources.resolve((String)a[0]),target,StandardCopyOption.REPLACE_EXISTING); yield null; }
            case "getLogger" -> Logger.getLogger("verification");
            case "getName" -> "CavesLite";
            default -> throw new UnsupportedOperationException(m.getName());
        });
        org.yaml.snakeyaml.LoaderOptions yamlOptions = new org.yaml.snakeyaml.LoaderOptions();
        yamlOptions.setAllowDuplicateKeys(false);
        var strictYaml = new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(yamlOptions));
        try (var paths=Files.walk(root)) {
            for(Path path:paths.filter(p->p.toString().endsWith(".yml") && !p.startsWith(root.resolve("build"))).toList()) {
                strictYaml.load(Files.readString(path)); check(true,"Strict YAML: "+path);
            }
        }
        Lang lang = new Lang(plugin);
        YamlConfiguration es = new YamlConfiguration(), en = new YamlConfiguration();
        es.load(resources.resolve("lang/es.yml").toFile()); en.load(resources.resolve("lang/en.yml").toFile());
        check(es.getKeys(true).equals(en.getKeys(true)),"Language key parity");
        for (String code : List.of("es","en")) {
            lang.reload(code);
            try (var paths=Files.walk(resources)) {
                for(Path path:paths.filter(p->p.toString().endsWith(".yml")).toList()) {
                    YamlConfiguration yaml=new YamlConfiguration();yaml.load(path.toFile());
                    for(String key:yaml.getKeys(true)) if(yaml.get(key) instanceof String value && value.startsWith("@lang:")) {
                        check(!lang.resolve(value).contains("missing lang key"),path+":"+key);
                    }
                }
            }
            try(var paths=Files.walk(root.resolve("src/main/java"))) {
                Pattern pattern=Pattern.compile("(?:lang|plugin\\.getLanguage\\(\\))\\.get\\(\"([^\"]+)\"");
                for(Path path:paths.filter(p->p.toString().endsWith(".java")).toList()) {
                    Matcher matcher=pattern.matcher(Files.readString(path));
                    while(matcher.find())check(!lang.get(matcher.group(1)).contains("missing lang key"),matcher.group(1));
                }
            }
            var cfg=YamlConfiguration.loadConfiguration(resources.resolve("config.yml").toFile());
            var display=lang.displayCopy(cfg,"config");
            check(!display.getString("spawn-titles.subtitle").startsWith("@lang:"),"Resolved format before placeholders");
            check(display.getString("spawn-titles.subtitle").contains("{mob}"),"Preserve mob placeholder");
            check(cfg.getString("spawn-titles.subtitle").startsWith("@lang:"),"Source configuration unmodified");
            var legacy=YamlConfiguration.loadConfiguration(resources.resolve("legacy-display.yml").toFile());
            check(lang.displayCopy(legacy.getConfigurationSection("config"),"config").getString("boss-bar.format").equals(lang.get("config.boss-bar.format")),"Old stock config translation");
            cfg.set("boss-bar.format","CUSTOM {mob}");
            check(lang.displayCopy(cfg,"config").getString("boss-bar.format").equals("CUSTOM {mob}"),"Custom text preserved");
            int count=0;
            for(String key:cfg.getConfigurationSection("mobs").getKeys(false)) {
                if(cfg.isSet("mobs."+key+".nether-enabled")){ count++;check(!cfg.getBoolean("mobs."+key+".nether-enabled"),"Nether defaults OFF: "+key); }
            }
            check(count==18,"All 18 mobs have Nether switch");
        }
        // User English overrides survive, missing keys fall through to bundled English.
        Files.writeString(data.resolve("lang/en.yml"),"command:\n  reload: CUSTOM\n");
        lang.reload("en");check(lang.get("command.reload").equals("CUSTOM"),"User override");
        check(lang.get("command.list").contains("Custom mobs"),"Bundled English per-key fallback");
        check(lang.get("command.list",Map.of("mobs","test")).contains("test"),"Placeholder replacement");
        lang.reload("unknown");check(lang.get("command.list").contains("Mobs personalizados"),"Unknown language Spanish fallback");
        lang.reload("../../escape");check(lang.code().equals("es"),"Language path validation");
        check(lang.get("does.not.exist").contains("missing lang key"),"Visible missing key marker");
        System.out.println("PASS: "+checks+" checks (YAML, language references, fallbacks, placeholders, config migration, Nether defaults)");
    }
}
