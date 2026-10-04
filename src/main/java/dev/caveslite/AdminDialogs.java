package dev.caveslite;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobManager;
import dev.caveslite.util.Text;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Native Dialog admin UI. dialogs.yml controls presentation; config.yml is gameplay truth. */
public final class AdminDialogs {
    private final CavesLite plugin;
    private final MobManager mobs;
    private final Map<String, Material> icons = new LinkedHashMap<>();
    private YamlConfiguration ui;

    public AdminDialogs(CavesLite plugin, MobManager mobs) {
        this.plugin = plugin; this.mobs = mobs;
        File file = new File(plugin.getDataFolder(), "dialogs.yml");
        if (!file.exists()) plugin.saveResource("dialogs.yml", false);
        reload();
    }

    public void reload() {
        ui = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "dialogs.yml"));
        icons.clear();
        for (String id : mobs.getMobIds()) {
            Material fallback = Material.SPAWNER;
            Material material = Material.matchMaterial(ui.getString("icons." + id, fallback.name()));
            icons.put(id, material == null ? fallback : material);
        }
    }

    public void showMain(Player player) { player.showDialog(buildMain()); }

    private Dialog buildMain() {
        List<ActionButton> actions = new ArrayList<>();
        for (String id : mobs.getMobIds()) {
            boolean enabled = plugin.getConfig().getBoolean("mobs." + id + ".enabled", false);
            String status = ui.getString(enabled ? "main.active-prefix" : "main.inactive-prefix", enabled ? "&a[◆] &f" : "&c[◆] &f");
            String name = plainConfiguredName(id);
            actions.add(ActionButton.builder(Text.legacy(status + name))
                    .tooltip(Text.legacy((enabled ? "&aActivo &8- &f" : "&cInactivo &8- &f") + name))
                    .action(DialogAction.customClick((view, audience) -> { if (audience instanceof Player p) showMob(p, id); }, oneUse())).build());
        }
        boolean hEnabled = plugin.getConfig().getBoolean("herobrine.enabled", false);
        String hPrefix = ui.getString(hEnabled ? "main.active-prefix" : "main.inactive-prefix", hEnabled ? "&a[◆] &f" : "&c[◆] &f");
        String hName = stripLegacyCodes(ui.getString("main.herobrine-label", "Herobrine"));
        actions.add(ActionButton.builder(Text.legacy(hPrefix + hName))
                .tooltip(Text.legacy(hEnabled ? "&aActivo" : "&cInactivo"))
                .action(DialogAction.customClick((view, audience) -> { if (audience instanceof Player p) showHerobrine(p); }, oneUse())).build());
        return Dialog.create(b -> b.empty().base(DialogBase.builder(Text.legacy(ui.getString("main.title", "&4&lCavesLite")))
                .body(mainBody()).build()).type(DialogType.multiAction(actions).columns(Math.max(1, Math.min(10, ui.getInt("main.columns", 3)))).build()));
    }

    private List<DialogBody> mainBody() {
        List<DialogBody> body = new ArrayList<>();
        Material material = Material.matchMaterial(ui.getString("main.decorative-item", "SCULK_CATALYST"));
        String description = ui.getString("main.description", "&7Criaturas, encuentros y herramientas");
        if (material != null) {
            // A Dialog item body is a vertical body entry, not a horizontal layout cell.
            // Keep one decorative item so the header remains compact and visually aligned.
            body.add(DialogBody.item(new ItemStack(material), DialogBody.plainMessage(Text.legacy(description)), true, true, 40, 40));
        } else body.add(DialogBody.plainMessage(Text.legacy(description)));
        return body;
    }

    /** Global switches. Detailed tuning remains in config.yml and DOCUMENTATION.md. */
    public void showOptions(Player player) {
        String ambientPath = "caverns.ambient.enabled", footstepsPath = "caverns.ambient.footsteps.enabled";
        List<DialogBody> body = List.of(DialogBody.plainMessage(Text.legacy(ui.getString("options.description",
                "&7Los checks se aplican al guardar. Ajustes avanzados: &fconfig.yml&7. Consulta &fDOCUMENTATION.md&7."))));
        Dialog dialog = Dialog.create(b -> b.empty().base(DialogBase.builder(Text.legacy(ui.getString("options.title", "&4&lCavesLite &8- &fOpciones")))
                .body(body).inputs(List.of(
                        bool("ambient", "options.labels.ambient", "&bSonidos ambientales", plugin.getConfig().getBoolean(ambientPath, true)),
                        bool("footsteps", "options.labels.footsteps", "&bPasos fantasma", plugin.getConfig().getBoolean(footstepsPath, true)),
                        bool("tension", "options.labels.tension", "&cTension / latidos", plugin.getConfig().getBoolean("tension.enabled", true)),
                        bool("bossbar", "options.labels.bossbar", "&dBossBars", plugin.getConfig().getBoolean("boss-bar.enabled", false)),
                        bool("spawntitles", "options.labels.spawn-titles", "&eTitulos de aparicion", plugin.getConfig().getBoolean("spawn-titles.enabled", true)),
                        bool("warnings", "options.labels.warnings", "&6Avisos ActionBar", plugin.getConfig().getBoolean("warnings.enabled", true)),
                        bool("trails", "options.labels.trails", "&aTrails / particulas", plugin.getConfig().getBoolean("trails.enabled", true)),
                        bool("achievements", "options.labels.achievements", "&6Achievements", plugin.getConfig().getBoolean("achievements.enabled", true)),
                        bool("leaderboards", "options.labels.leaderboards", "&eLeaderboards", plugin.getConfig().getBoolean("leaderboards.enabled", true))
                )).build()).type(DialogType.multiAction(List.of(button(ui.getString("options.buttons.save", "&a✔ Guardar"), (view,audience)->{
                    if (!(audience instanceof Player p)) return;
                    plugin.getConfig().set(ambientPath, yes(view.getBoolean("ambient"))); plugin.getConfig().set(footstepsPath, yes(view.getBoolean("footsteps")));
                    plugin.getConfig().set("tension.enabled", yes(view.getBoolean("tension"))); plugin.getConfig().set("boss-bar.enabled", yes(view.getBoolean("bossbar")));
                    plugin.getConfig().set("spawn-titles.enabled", yes(view.getBoolean("spawntitles"))); plugin.getConfig().set("warnings.enabled", yes(view.getBoolean("warnings")));
                    plugin.getConfig().set("trails.enabled", yes(view.getBoolean("trails"))); plugin.getConfig().set("achievements.enabled", yes(view.getBoolean("achievements")));
                    plugin.getConfig().set("leaderboards.enabled", yes(view.getBoolean("leaderboards"))); saveReload();
                    p.sendMessage(Text.legacy(ui.getString("options.saved", "&aOpciones globales actualizadas.")));
                }))).columns(1).build()));
        player.showDialog(dialog);
    }

    private void showMob(Player player, String id) {
        CustomMob mob = mobs.getMob(id); if (mob == null) return;
        String path="mobs."+id+"."; String name=configuredName(id);
        int priority=plugin.getConfig().getInt(path+"priority",0), minY=plugin.getConfig().getInt(path+"spawn-y-min", id.equals("giant-zombie")?80:plugin.getConfig().getInt("mobs.y-min",-64));
        int maxY=plugin.getConfig().getInt(path+"spawn-y-max", id.equals("giant-zombie")?320:plugin.getConfig().getInt("mobs.y-max",64));
        int cooldown=plugin.getConfig().getInt(path+"cooldown-seconds",0), maxActive=plugin.getConfig().getInt(path+"max-active",0);
        List<DialogInput> inputs=new ArrayList<>(); String l="mob.labels.";
        inputs.add(bool("enabled",l+"enabled","&aSpawn natural",plugin.getConfig().getBoolean(path+"enabled",false)));
        inputs.add(DialogInput.text("name",Text.legacy(ui.getString(l+"name","&fNombre"))).initial(name).maxLength(128).build());
        inputs.add(bool("showname",l+"show-name","&fMostrar nombre",plugin.getConfig().getBoolean(path+"show-name",false)));
        inputs.add(DialogInput.numberRange("priority",Text.legacy(ui.getString(l+"priority","&ePeso de seleccion (0-10)")),0,10).step(1f).initial((float)Math.min(10,Math.max(0,priority))).build());
        inputs.add(bool("bossbar",l+"bossbar","&dBossBar",plugin.getConfig().getBoolean("boss-bar.mobs."+id,false)));
        inputs.add(range("health","&cVida",1,200,plugin.getConfig().getDouble(path+"health",20)));
        inputs.add(DialogInput.numberRange("ymin",Text.legacy(ui.getString(l+"ymin","&7Altura minima")),-64,320).step(1f).initial((float)minY).build());
        inputs.add(DialogInput.numberRange("ymax",Text.legacy(ui.getString(l+"ymax","&7Altura maxima")),-64,320).step(1f).initial((float)maxY).build());
        inputs.add(DialogInput.numberRange("cooldown",Text.legacy(ui.getString(l+"cooldown","&7Cooldown global (segundos)")),0,1800).step(5f).initial((float)Math.min(1800,cooldown)).build());
        inputs.add(DialogInput.numberRange("maxactive",Text.legacy(ui.getString(l+"maxactive","&7Maximo activo global (0 = sin limite)")),0,20).step(1f).initial((float)Math.min(20,maxActive)).build());
        addSpecificInputs(inputs,id,path);
        String dropPath=(id.equals("dead-miner")||id.equals("mimic"))?path+"drop-items":path+"extra-drops";
        inputs.add(DialogInput.text("drops",Text.legacy("&6Drops CavesLite (MATERIAL, MATERIAL)")).initial(String.join(", ",plugin.getConfig().getStringList(dropPath))).maxLength(2048).build());
        inputs.add(bool("deathcmd", "", "&6Ejecutar comando al morir", plugin.getConfig().getBoolean(path+"death-command.enabled",false)));
        inputs.add(DialogInput.text("deathcommand",Text.legacy("&7Comando consola (%player%, %mob%, %world%, %x%, %y%, %z%)")).initial(plugin.getConfig().getString(path+"death-command.command","")).maxLength(1024).build());

        List<DialogBody> body=List.of(DialogBody.item(new ItemStack(icons.getOrDefault(id,Material.SPAWNER)),DialogBody.plainMessage(Text.legacy(
                "&7Prioridad = peso relativo, no porcentaje. Cooldown y maximo activo son globales. Guardar aplica inmediatamente.")),true,true,48,48));
        Dialog dialog=Dialog.create(b->b.empty().base(DialogBase.builder(Text.legacy(ui.getString("mob.title-prefix","&4&l")+name)).body(body).inputs(inputs).build())
                .type(DialogType.multiAction(List.of(
                        button(ui.getString("mob.buttons.save","&a✔ Guardar"),(view,audience)->{ if(!(audience instanceof Player p))return;
                            int newMin=Math.round(orFloat(view.getFloat("ymin"),minY)), newMax=Math.round(orFloat(view.getFloat("ymax"),maxY)); if(newMin>newMax){int t=newMin;newMin=newMax;newMax=t;}
                            plugin.getConfig().set(path+"enabled",yes(view.getBoolean("enabled"))); plugin.getConfig().set(path+"name",view.getText("name")); plugin.getConfig().set(path+"show-name",yes(view.getBoolean("showname")));
                            plugin.getConfig().set(path+"priority",Math.max(0,Math.min(10,Math.round(orFloat(view.getFloat("priority"),priority))))); plugin.getConfig().set(path+"spawn-y-min",newMin); plugin.getConfig().set(path+"spawn-y-max",newMax);
                            plugin.getConfig().set(path+"cooldown-seconds",Math.max(0,Math.round(orFloat(view.getFloat("cooldown"),cooldown)))); plugin.getConfig().set(path+"max-active",Math.max(0,Math.round(orFloat(view.getFloat("maxactive"),maxActive))));
                            boolean nb=yes(view.getBoolean("bossbar")); plugin.getConfig().set("boss-bar.mobs."+id,nb); if(nb)plugin.getConfig().set("boss-bar.enabled",true); setNum(path+"health",view,"health");
                            saveSpecific(view,id,path); plugin.getConfig().set(dropPath,parseList(view.getText("drops"))); plugin.getConfig().set(path+"death-command.enabled",yes(view.getBoolean("deathcmd"))); plugin.getConfig().set(path+"death-command.command",view.getText("deathcommand"));
                            saveReload(); p.sendMessage(Text.legacy("&aGuardado: &f"+configuredName(id)));
                        }),
                        button(ui.getString("mob.buttons.summon","&e⚡ Invocar"),(view,audience)->{if(audience instanceof Player p)mobs.spawn(id,p.getLocation());}),
                        button(ui.getString("mob.buttons.remove","&c✖ Eliminar"),(view,audience)->{if(audience instanceof Player p){int n=mobs.killAll(e->dev.caveslite.util.TagHelper.isTagged(e,id));n+=mobs.cleanupArtifacts(id);p.sendMessage(Text.legacy("&7Eliminados: &f"+n));}})
                )).columns(3).build())); player.showDialog(dialog);
    }

    private void addSpecificInputs(List<DialogInput> in,String id,String p){
        switch(id){
            case "alpha-spider" -> { in.add(range("cobweb","&7Probabilidad telaraña %",0,100,plugin.getConfig().getDouble(p+"cobweb-chance",14.29))); in.add(range("minion","&7Probabilidad minion %",0,100,plugin.getConfig().getDouble(p+"minion-chance",6.67))); }
            case "crying-bat" -> { in.add(range("cry","&7Probabilidad de grito %",0,100,plugin.getConfig().getDouble(p+"cry-chance",3.33))); in.add(range("death","&7Probabilidad de morir tras gritar %",0,100,plugin.getConfig().getDouble(p+"death-chance",20))); }
            case "dead-miner" -> { in.add(bool("target","","&7Requiere objetivo",plugin.getConfig().getBoolean(p+"requires-target",true))); in.add(bool("torches","","&7Colocar antorchas",plugin.getConfig().getBoolean(p+"place-torches",true))); in.add(bool("redtorches","","&cAntorchas redstone",plugin.getConfig().getBoolean(p+"red-torches",false))); in.add(range("dropchance","&6Probabilidad drop al recibir daño %",0,100,plugin.getConfig().getDouble(p+"drop-chance",30))); in.add(range("torchcooldown","&7Cooldown antorchas (segundos)",0,300,plugin.getConfig().getInt(p+"torches-cooldown",12))); in.add(DialogInput.text("headvalue",Text.legacy("&eTextura cabeza Base64")).initial(plugin.getConfig().getString(p+"head-value","")).maxLength(8192).build()); }
            case "watcher" -> in.add(DialogInput.text("headvalue",Text.legacy("&eTextura cabeza Base64")).initial(plugin.getConfig().getString(p+"head-value","")).maxLength(8192).build());
            case "hexed-armor" -> { in.add(bool("binding","","&5Maldicion de ligamiento",plugin.getConfig().getBoolean(p+"binding-curse",true))); in.add(range("applychance","&5Probabilidad intercambio al golpear %",0,100,plugin.getConfig().getDouble(p+"apply-chance",25))); }
            case "hungering-darkness" -> { in.add(bool("removeonlight","","&7Eliminar con luz",plugin.getConfig().getBoolean(p+"remove-on-light",false))); in.add(bool("deathsound","","&7Sonido al desaparecer con luz",plugin.getConfig().getBoolean(p+"death-sound",true))); in.add(bool("nightvision","","&7Vision nocturna cuenta como luz",plugin.getConfig().getBoolean(p+"night-vision",false))); in.add(range("darkdamage","&cDaño",0,200,plugin.getConfig().getDouble(p+"damage",200))); }
            case "lava-creeper" -> { in.add(range("firetouch","&6Fire touch (ticks)",0,200,plugin.getConfig().getInt(p+"fire-touch",10))); in.add(range("radius","&6Radio",1,16,plugin.getConfig().getInt(p+"radius",4))); in.add(range("changechance","&6Probabilidad cambiar bloque %",0,100,plugin.getConfig().getDouble(p+"change-chance",50))); }
            case "smoke-demon" -> { in.add(range("maxlight","&7Luz maxima",0,15,plugin.getConfig().getInt(p+"max-light",11))); in.add(range("harmradius","&7Radio de daño",1,16,plugin.getConfig().getInt(p+"harm-radius",3))); in.add(DialogInput.text("particle",Text.legacy("&7Particula")).initial(plugin.getConfig().getString(p+"particle","CLOUD")).maxLength(64).build()); }
            case "magma-monster" -> { in.add(bool("extdamage","","&6Daño al extinguirse",plugin.getConfig().getBoolean(p+"extinguished-damage",false))); in.add(bool("target","","&7Requiere objetivo",plugin.getConfig().getBoolean(p+"requires-target",true))); in.add(range("firechance","&6Probabilidad fuego %",0,100,plugin.getConfig().getDouble(p+"fire-chance",7.14))); in.add(range("magmachance","&6Probabilidad magma %",0,100,plugin.getConfig().getDouble(p+"magma-chance",3.57))); }
            case "tnt-creeper" -> { in.add(range("tntamount","&cCantidad TNT",0,10,plugin.getConfig().getInt(p+"tnt-amount",2))); in.add(range("explosionchance","&cProbabilidad explosion al recibir daño %",0,100,plugin.getConfig().getDouble(p+"explosion-chance",33.33))); }
            case "chicken-jockey" -> in.add(range("speedamp","&eAmplificador de velocidad",0,10,plugin.getConfig().getInt(p+"speed-amplifier",3)));
            case "mimic" -> { in.add(range("chestdistance","&6Distancia minima de otro cofre",0,64,plugin.getConfig().getInt(p+"min-distance-from-chest",16))); in.add(range("maxchests","&6Maximo cofres Mimic por mundo",0,20,plugin.getConfig().getInt(p+"max-active-chests",1))); in.add(range("lifetime","&6Vida del cofre (segundos)",0,3600,plugin.getConfig().getLong(p+"lifetime-seconds",900))); in.add(bool("removeunload","","&6Eliminar cofre al descargar chunk",plugin.getConfig().getBoolean(p+"remove-on-unload",true))); }
            case "cave-golem" -> { in.add(bool("slowness","","&7Lentitud",plugin.getConfig().getBoolean(p+"slowness",true))); in.add(bool("distract","","&7Efectos al atacar",plugin.getConfig().getBoolean(p+"distract-attack",true))); in.add(range("blockspawn","&7Spawn desde bloque %",0,100,plugin.getConfig().getDouble(p+"spawn-from-block",0.5))); in.add(range("damagemod","&cMultiplicador daño ataque",0,10,plugin.getConfig().getDouble(p+"damage-modifier",2))); in.add(range("nonpick","&7Multiplicador daño sin pico",0,2,plugin.getConfig().getDouble(p+"nonpickaxe-modifier",0.07))); in.add(range("pick","&7Multiplicador daño con pico",0,10,plugin.getConfig().getDouble(p+"pickaxe-modifier",2))); in.add(DialogInput.text("variants",Text.legacy("&6Variantes/drops (MATERIAL, MATERIAL)")).initial(String.join(", ",plugin.getConfig().getStringList(p+"variants"))).maxLength(2048).build()); }
            default -> { }
        }
    }

    private void saveSpecific(DialogResponseView v,String id,String p){
        switch(id){
            case "alpha-spider" -> {setNum(p+"cobweb-chance",v,"cobweb");setNum(p+"minion-chance",v,"minion");}
            case "crying-bat" -> {setNum(p+"cry-chance",v,"cry");setNum(p+"death-chance",v,"death");}
            case "dead-miner" -> {plugin.getConfig().set(p+"requires-target",yes(v.getBoolean("target")));plugin.getConfig().set(p+"place-torches",yes(v.getBoolean("torches")));plugin.getConfig().set(p+"red-torches",yes(v.getBoolean("redtorches")));setNum(p+"drop-chance",v,"dropchance");setNum(p+"torches-cooldown",v,"torchcooldown");plugin.getConfig().set(p+"head-value",v.getText("headvalue"));}
            case "watcher" -> plugin.getConfig().set(p+"head-value",v.getText("headvalue"));
            case "hexed-armor" -> {plugin.getConfig().set(p+"binding-curse",yes(v.getBoolean("binding")));setNum(p+"apply-chance",v,"applychance");}
            case "hungering-darkness" -> {plugin.getConfig().set(p+"remove-on-light",yes(v.getBoolean("removeonlight")));plugin.getConfig().set(p+"death-sound",yes(v.getBoolean("deathsound")));plugin.getConfig().set(p+"night-vision",yes(v.getBoolean("nightvision")));setNum(p+"damage",v,"darkdamage");}
            case "lava-creeper" -> {setNum(p+"fire-touch",v,"firetouch");setNum(p+"radius",v,"radius");setNum(p+"change-chance",v,"changechance");}
            case "smoke-demon" -> {setNum(p+"max-light",v,"maxlight");setNum(p+"harm-radius",v,"harmradius");plugin.getConfig().set(p+"particle",v.getText("particle"));}
            case "magma-monster" -> {plugin.getConfig().set(p+"extinguished-damage",yes(v.getBoolean("extdamage")));plugin.getConfig().set(p+"requires-target",yes(v.getBoolean("target")));setNum(p+"fire-chance",v,"firechance");setNum(p+"magma-chance",v,"magmachance");}
            case "tnt-creeper" -> {setNum(p+"tnt-amount",v,"tntamount");setNum(p+"explosion-chance",v,"explosionchance");}
            case "chicken-jockey" -> setNum(p+"speed-amplifier",v,"speedamp");
            case "mimic" -> {setNum(p+"min-distance-from-chest",v,"chestdistance");setNum(p+"max-active-chests",v,"maxchests");setNum(p+"lifetime-seconds",v,"lifetime");plugin.getConfig().set(p+"remove-on-unload",yes(v.getBoolean("removeunload")));}
            case "cave-golem" -> {plugin.getConfig().set(p+"slowness",yes(v.getBoolean("slowness")));plugin.getConfig().set(p+"distract-attack",yes(v.getBoolean("distract")));setNum(p+"spawn-from-block",v,"blockspawn");setNum(p+"damage-modifier",v,"damagemod");setNum(p+"nonpickaxe-modifier",v,"nonpick");setNum(p+"pickaxe-modifier",v,"pick");plugin.getConfig().set(p+"variants",parseList(v.getText("variants")));}
            default -> { }
        }
    }

    private void showHerobrine(Player player){
        String p="herobrine."; List<DialogInput> in=new ArrayList<>();
        in.add(bool("enabled","","&aEncuentros naturales",plugin.getConfig().getBoolean(p+"enabled",false)));
        in.add(range("ymin","&7Altura minima",-64,320,plugin.getConfig().getInt(p+"y-min",-64))); in.add(range("ymax","&7Altura maxima",-64,320,plugin.getConfig().getInt(p+"y-max",320)));
        in.add(range("cooldown","&7Cooldown (segundos)",0,1800,plugin.getConfig().getLong(p+"cooldown-seconds",120))); in.add(range("intervalmin","&7Intervalo minimo (segundos)",30,3600,plugin.getConfig().getLong(p+"encounter-interval.min-seconds",480))); in.add(range("intervalmax","&7Intervalo maximo (segundos)",30,7200,plugin.getConfig().getLong(p+"encounter-interval.max-seconds",1200)));
        in.add(range("mindistance","&7Distancia minima jugador",1,128,plugin.getConfig().getDouble(p+"min-distance-from-player",10))); in.add(range("maxdistance","&7Distancia maxima jugador",2,256,plugin.getConfig().getDouble(p+"max-distance-from-player",24)));
        in.add(bool("bossbar","","&dBossBar de combate",plugin.getConfig().getBoolean(p+"combat.bossbar-enabled",false))); in.add(range("health","&cVida",1,200,plugin.getConfig().getDouble(p+"health",80))); in.add(range("damage","&cDaño",0,20,plugin.getConfig().getDouble(p+"damage",2)));
        in.add(bool("sneaky","","&cSneaky strike",plugin.getConfig().getBoolean(p+"stalking.sneaky-strike.enabled",true))); in.add(range("sneakychance","&cSneaky strike %",0,100,plugin.getConfig().getDouble(p+"stalking.sneaky-strike.chance",10)));
        in.add(bool("torches","","&cAntorchas redstone",plugin.getConfig().getBoolean(p+"redstone-torches.enabled",true))); in.add(range("torchchance","&cProbabilidad antorcha %",0,100,plugin.getConfig().getDouble(p+"redstone-torches.chance",12)));
        in.add(bool("rewardcmd","","&6Comando al morir",plugin.getConfig().getBoolean(p+"reward.command.enabled",false))); in.add(DialogInput.text("rewardcommand",Text.legacy("&7Comando consola ({player})")).initial(plugin.getConfig().getString(p+"reward.command.command","")).maxLength(1024).build());
        in.add(DialogInput.text("skin",Text.legacy("&eSkin Base64")).initial(plugin.getConfig().getString(p+"skin-value","")).maxLength(8192).build());
        Dialog d=Dialog.create(b->b.empty().base(DialogBase.builder(Text.legacy("&4&lHerobrine")).body(List.of(DialogBody.item(new ItemStack(Material.PLAYER_HEAD),DialogBody.plainMessage(Text.legacy("&7Configuracion del encuentro especial. Guardar aplica inmediatamente.")),true,true,48,48))).inputs(in).build()).type(DialogType.multiAction(List.of(
                button("&a✔ Guardar",(v,a)->{if(!(a instanceof Player pl))return; plugin.getConfig().set(p+"enabled",yes(v.getBoolean("enabled"))); setNum(p+"y-min",v,"ymin");setNum(p+"y-max",v,"ymax");setNum(p+"cooldown-seconds",v,"cooldown");setNum(p+"encounter-interval.min-seconds",v,"intervalmin");setNum(p+"encounter-interval.max-seconds",v,"intervalmax");setNum(p+"min-distance-from-player",v,"mindistance");setNum(p+"max-distance-from-player",v,"maxdistance");plugin.getConfig().set(p+"combat.bossbar-enabled",yes(v.getBoolean("bossbar")));setNum(p+"health",v,"health");setNum(p+"damage",v,"damage");plugin.getConfig().set(p+"stalking.sneaky-strike.enabled",yes(v.getBoolean("sneaky")));setNum(p+"stalking.sneaky-strike.chance",v,"sneakychance");plugin.getConfig().set(p+"redstone-torches.enabled",yes(v.getBoolean("torches")));setNum(p+"redstone-torches.chance",v,"torchchance");plugin.getConfig().set(p+"reward.command.enabled",yes(v.getBoolean("rewardcmd")));plugin.getConfig().set(p+"reward.command.command",v.getText("rewardcommand"));plugin.getConfig().set(p+"skin-value",v.getText("skin"));saveReload();pl.sendMessage(Text.legacy("&aHerobrine actualizado."));}),
                button("&e⚡ Invocar",(v,a)->{if(a instanceof Player pl)plugin.getHerobrine().summonFor(pl,"stalking");}), button("&c✖ Eliminar",(v,a)->{if(a instanceof Player pl){plugin.getHerobrine().removeAll();pl.sendMessage(Text.legacy("&7Herobrine eliminado."));}}))).columns(3).build())); player.showDialog(d);
    }

    private DialogInput bool(String id,String key,String fallback,boolean value){return DialogInput.bool(id,Text.legacy(key.isEmpty()?fallback:ui.getString(key,fallback)),value,"true","false");}
    private DialogInput range(String id,String label,float min,float max,double value){return DialogInput.numberRange(id,Text.legacy(label),min,max).step(1f).initial((float)Math.max(min,Math.min(max,value))).build();}
    private void setNum(String path,DialogResponseView v,String id){Float f=v.getFloat(id);if(f!=null)plugin.getConfig().set(path,f.doubleValue());}
    private boolean yes(Boolean b){return Boolean.TRUE.equals(b);} private float orFloat(Float v,float f){return v==null?f:v;}
    private List<String> parseList(String raw){if(raw==null||raw.isBlank())return new ArrayList<>();List<String> out=new ArrayList<>();for(String x:raw.split("[,\\n]")){String v=x.trim().toUpperCase();if(!v.isEmpty())out.add(v);}return out;}
    private String configuredName(String id){String raw=plugin.getConfig().getString("mobs."+id+".name",pretty(id));return raw==null||raw.isBlank()?pretty(id):raw;}
    private String plainConfiguredName(String id){ return stripLegacyCodes(configuredName(id)); }
    private String stripLegacyCodes(String value){ return value == null ? "" : value.replaceAll("(?i)&[0-9A-FK-ORX]", ""); }
    private void saveReload(){plugin.saveConfig();plugin.reloadAll();}
    private ActionButton button(String label,DialogActionCallback cb){return ActionButton.builder(Text.legacy(label)).action(DialogAction.customClick(cb,oneUse())).build();}
    private ClickCallback.Options oneUse(){return ClickCallback.Options.builder().uses(1).build();}
    private String pretty(String id){StringBuilder o=new StringBuilder();for(String part:id.split("-"))o.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');return o.toString().trim();}
}
