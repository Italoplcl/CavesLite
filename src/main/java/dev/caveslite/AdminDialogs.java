package dev.caveslite;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobManager;
import dev.caveslite.util.DialogText;
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
        ui = plugin.getLanguage().displayCopy(YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "dialogs.yml")), "dialogs");
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
            String name = plainConfiguredName(id);
            actions.add(ActionButton.builder(DialogText.status(name, enabled))
                    .tooltip(DialogText.component((enabled ? plugin.getLanguage().get("admindialogs.text-1") : plugin.getLanguage().get("admindialogs.text-2")) + name))
                    .action(DialogAction.customClick((view, audience) -> { if (audience instanceof Player p) showMob(p, id); }, oneUse())).build());
        }
        boolean hEnabled = plugin.getConfig().getBoolean("herobrine.enabled", false);
        String hName = stripLegacyCodes(ui.getString("main.herobrine-label", "Herobrine"));
        actions.add(ActionButton.builder(DialogText.status(hName, hEnabled))
                .tooltip(DialogText.component(hEnabled ? plugin.getLanguage().get("admindialogs.text-3") : plugin.getLanguage().get("admindialogs.text-4")))
                .action(DialogAction.customClick((view, audience) -> { if (audience instanceof Player p) showHerobrine(p); }, oneUse())).build());
        return Dialog.create(b -> b.empty().base(DialogBase.builder(DialogText.component(ui.getString("main.title", plugin.getLanguage().get("admindialogs.text-5"))))
                .body(mainBody()).build()).type(DialogType.multiAction(actions).columns(Math.max(1, Math.min(10, ui.getInt("main.columns", 3)))).build()));
    }

    private List<DialogBody> mainBody() {
        List<DialogBody> body = new ArrayList<>();
        Material material = Material.matchMaterial(ui.getString("main.decorative-item", "SCULK_CATALYST"));
        String description = ui.getString("main.description", plugin.getLanguage().get("admindialogs.text-6"));
        if (material != null) {
            // A Dialog item body is a vertical body entry, not a horizontal layout cell.
            // Keep one decorative item so the header remains compact and visually aligned.
            body.add(DialogBody.item(new ItemStack(material), DialogBody.plainMessage(DialogText.component(description)), true, true, 40, 40));
        } else body.add(DialogBody.plainMessage(DialogText.component(description)));
        return body;
    }

    /** Global switches. Detailed tuning remains in config.yml and DOCUMENTATION.md. */
    public void showOptions(Player player) {
        String ambientPath = "caverns.ambient.enabled", footstepsPath = "caverns.ambient.footsteps.enabled";
        List<DialogBody> body = List.of(DialogBody.plainMessage(DialogText.component(ui.getString("options.description",
                plugin.getLanguage().get("admindialogs.text-7")))));
        Dialog dialog = Dialog.create(b -> b.empty().base(DialogBase.builder(DialogText.component(ui.getString("options.title", plugin.getLanguage().get("admindialogs.text-8"))))
                .body(body).inputs(List.of(
                        bool("ambient", "options.labels.ambient", plugin.getLanguage().get("admindialogs.text-9"), plugin.getConfig().getBoolean(ambientPath, true)),
                        bool("footsteps", "options.labels.footsteps", plugin.getLanguage().get("admindialogs.text-10"), plugin.getConfig().getBoolean(footstepsPath, true)),
                        bool("tension", "options.labels.tension", plugin.getLanguage().get("admindialogs.text-11"), plugin.getConfig().getBoolean("tension.enabled", true)),
                        bool("bossbar", "options.labels.bossbar", plugin.getLanguage().get("admindialogs.text-12"), plugin.getConfig().getBoolean("boss-bar.enabled", false)),
                        bool("spawntitles", "options.labels.spawn-titles", plugin.getLanguage().get("admindialogs.text-13"), plugin.getConfig().getBoolean("spawn-titles.enabled", true)),
                        bool("warnings", "options.labels.warnings", plugin.getLanguage().get("admindialogs.text-14"), plugin.getConfig().getBoolean("warnings.enabled", true)),
                        bool("trails", "options.labels.trails", plugin.getLanguage().get("admindialogs.text-15"), plugin.getConfig().getBoolean("trails.enabled", true)),
                        bool("achievements", "options.labels.achievements", plugin.getLanguage().get("admindialogs.text-16"), plugin.getConfig().getBoolean("achievements.enabled", true)),
                        bool("leaderboards", "options.labels.leaderboards", plugin.getLanguage().get("admindialogs.text-17"), plugin.getConfig().getBoolean("leaderboards.enabled", true))
                )).build()).type(DialogType.multiAction(List.of(button(ui.getString("options.buttons.save", plugin.getLanguage().get("admindialogs.text-18")), (view,audience)->{
                    if (!(audience instanceof Player p)) return;
                    plugin.getConfig().set(ambientPath, yes(view.getBoolean("ambient"))); plugin.getConfig().set(footstepsPath, yes(view.getBoolean("footsteps")));
                    plugin.getConfig().set("tension.enabled", yes(view.getBoolean("tension"))); plugin.getConfig().set("boss-bar.enabled", yes(view.getBoolean("bossbar")));
                    plugin.getConfig().set("spawn-titles.enabled", yes(view.getBoolean("spawntitles"))); plugin.getConfig().set("warnings.enabled", yes(view.getBoolean("warnings")));
                    plugin.getConfig().set("trails.enabled", yes(view.getBoolean("trails"))); plugin.getConfig().set("achievements.enabled", yes(view.getBoolean("achievements")));
                    plugin.getConfig().set("leaderboards.enabled", yes(view.getBoolean("leaderboards"))); saveReload();
                    p.sendMessage(DialogText.component(ui.getString("options.saved", plugin.getLanguage().get("admindialogs.text-19"))));
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
        inputs.add(bool("enabled",l+"enabled",plugin.getLanguage().get("admindialogs.text-20"),plugin.getConfig().getBoolean(path+"enabled",false)));
        inputs.add(bool("nether", "", plugin.getLanguage().get("nether.enabled-label"), plugin.getConfig().getBoolean(path+"nether-enabled", false)));
        inputs.add(DialogInput.text("name",DialogText.component(ui.getString(l+"name",plugin.getLanguage().get("admindialogs.text-21")))).initial(name).maxLength(128).build());
        inputs.add(bool("showname",l+"show-name",plugin.getLanguage().get("admindialogs.text-22"),plugin.getConfig().getBoolean(path+"show-name",false)));
        inputs.add(DialogInput.numberRange("priority",DialogText.component(ui.getString(l+"priority",plugin.getLanguage().get("admindialogs.text-23"))),0,10).step(1f).initial((float)Math.min(10,Math.max(0,priority))).build());
        inputs.add(bool("bossbar",l+"bossbar",plugin.getLanguage().get("admindialogs.text-24"),plugin.getConfig().getBoolean("boss-bar.mobs."+id,false)));
        inputs.add(range("health",plugin.getLanguage().get("admindialogs.text-25"),1,200,plugin.getConfig().getDouble(path+"health",20)));
        inputs.add(DialogInput.numberRange("ymin",DialogText.component(ui.getString(l+"ymin",plugin.getLanguage().get("admindialogs.text-26"))),-64,320).step(1f).initial((float)minY).build());
        inputs.add(DialogInput.numberRange("ymax",DialogText.component(ui.getString(l+"ymax",plugin.getLanguage().get("admindialogs.text-27"))),-64,320).step(1f).initial((float)maxY).build());
        inputs.add(DialogInput.numberRange("cooldown",DialogText.component(ui.getString(l+"cooldown",plugin.getLanguage().get("admindialogs.text-28"))),0,1800).step(5f).initial((float)Math.min(1800,cooldown)).build());
        inputs.add(DialogInput.numberRange("maxactive",DialogText.component(ui.getString(l+"maxactive",plugin.getLanguage().get("admindialogs.text-29"))),0,20).step(1f).initial((float)Math.min(20,maxActive)).build());
        addSpecificInputs(inputs,id,path);
        String dropPath=(id.equals("dead-miner")||id.equals("mimic"))?path+"drop-items":path+"extra-drops";
        inputs.add(DialogInput.text("drops",DialogText.component(plugin.getLanguage().get("admindialogs.text-30"))).initial(String.join(", ",plugin.getConfig().getStringList(dropPath))).maxLength(2048).build());
        inputs.add(bool("deathcmd", "", plugin.getLanguage().get("admindialogs.text-31"), plugin.getConfig().getBoolean(path+"death-command.enabled",false)));
        inputs.add(DialogInput.text("deathcommand",DialogText.component(plugin.getLanguage().get("admindialogs.text-32"))).initial(plugin.getConfig().getString(path+"death-command.command","")).maxLength(1024).build());

        List<DialogBody> body=List.of(DialogBody.item(new ItemStack(icons.getOrDefault(id,Material.SPAWNER)),DialogBody.plainMessage(DialogText.component(
                plugin.getLanguage().get("admindialogs.text-33"))),true,true,48,48));
        Dialog dialog=Dialog.create(b->b.empty().base(DialogBase.builder(DialogText.component(ui.getString("mob.title-prefix","&4&l")+name)).body(body).inputs(inputs).build())
                .type(DialogType.multiAction(List.of(
                        button(ui.getString("mob.buttons.save",plugin.getLanguage().get("admindialogs.text-34")),(view,audience)->{ if(!(audience instanceof Player p))return;
                            int newMin=Math.round(orFloat(view.getFloat("ymin"),minY)), newMax=Math.round(orFloat(view.getFloat("ymax"),maxY)); if(newMin>newMax){int t=newMin;newMin=newMax;newMax=t;}
                            plugin.getConfig().set(path+"nether-enabled",yes(view.getBoolean("nether"))); plugin.getConfig().set(path+"enabled",yes(view.getBoolean("enabled"))); if (!java.util.Objects.equals(view.getText("name"), name)) plugin.getConfig().set(path+"name",view.getText("name")); plugin.getConfig().set(path+"show-name",yes(view.getBoolean("showname")));
                            plugin.getConfig().set(path+"priority",Math.max(0,Math.min(10,Math.round(orFloat(view.getFloat("priority"),priority))))); plugin.getConfig().set(path+"spawn-y-min",newMin); plugin.getConfig().set(path+"spawn-y-max",newMax);
                            plugin.getConfig().set(path+"cooldown-seconds",Math.max(0,Math.round(orFloat(view.getFloat("cooldown"),cooldown)))); plugin.getConfig().set(path+"max-active",Math.max(0,Math.round(orFloat(view.getFloat("maxactive"),maxActive))));
                            boolean nb=yes(view.getBoolean("bossbar")); plugin.getConfig().set("boss-bar.mobs."+id,nb); if(nb)plugin.getConfig().set("boss-bar.enabled",true); setNum(path+"health",view,"health");
                            saveSpecific(view,id,path); plugin.getConfig().set(dropPath,parseList(view.getText("drops"))); plugin.getConfig().set(path+"death-command.enabled",yes(view.getBoolean("deathcmd"))); plugin.getConfig().set(path+"death-command.command",view.getText("deathcommand"));
                            saveReload(); p.sendMessage(DialogText.component(plugin.getLanguage().get("admindialogs.text-35")+configuredName(id)));
                        }),
                        button(ui.getString("mob.buttons.summon",plugin.getLanguage().get("admindialogs.text-36")),(view,audience)->{if(audience instanceof Player p && mobs.spawn(id,p.getLocation())==null)p.sendMessage(DialogText.component(plugin.getLanguage().get("command.summon-no-space")));}),
                        button(ui.getString("mob.buttons.remove",plugin.getLanguage().get("admindialogs.text-37")),(view,audience)->{if(audience instanceof Player p){int n=mobs.killAll(e->dev.caveslite.util.TagHelper.isTagged(e,id));n+=mobs.cleanupArtifacts(id);p.sendMessage(DialogText.component(plugin.getLanguage().get("admindialogs.text-38")+n));}})
                )).columns(3).build())); player.showDialog(dialog);
    }

    private void addSpecificInputs(List<DialogInput> in,String id,String p){
        switch(id){
            case "giant-zombie" -> {
                in.add(bool("giantmove","",plugin.getLanguage().get("giant-ui.movement"),plugin.getConfig().getBoolean(p+"movement.enabled",true)));
                in.add(bool("giantbasic","",plugin.getLanguage().get("giant-ui.basic"),plugin.getConfig().getBoolean(p+"attacks.basic.enabled",true)));
                in.add(bool("giantstomp","",plugin.getLanguage().get("giant-ui.stomp"),plugin.getConfig().getBoolean(p+"attacks.stomp.enabled",true)));
                in.add(bool("giantquake","",plugin.getLanguage().get("giant-ui.quake"),plugin.getConfig().getBoolean(p+"attacks.quake.enabled",true)));
                in.add(bool("giantminions","",plugin.getLanguage().get("giant-ui.minions"),plugin.getConfig().getBoolean(p+"reinforcements.enabled",true)));
                in.add(bool("giantloot","",plugin.getLanguage().get("giant-ui.loot"),plugin.getConfig().getBoolean(p+"loot.enabled",true)));
                in.add(DialogInput.numberRange("giantspeed",DialogText.component(plugin.getLanguage().get("giant-ui.speed")),0.01f,0.5f).step(0.01f).initial((float)plugin.getConfig().getDouble(p+"movement.speed",0.23)).build());
                in.add(range("giantbasicdamage",plugin.getLanguage().get("giant-ui.basic-damage"),0,100,plugin.getConfig().getDouble(p+"attacks.basic.damage",6)));
                in.add(range("giantstompdamage",plugin.getLanguage().get("giant-ui.stomp-damage"),0,100,plugin.getConfig().getDouble(p+"attacks.stomp.damage",8)));
                in.add(range("giantquakedamage",plugin.getLanguage().get("giant-ui.quake-damage"),0,100,plugin.getConfig().getDouble(p+"attacks.quake.damage",6)));
                in.add(range("giantstompradius",plugin.getLanguage().get("giant-ui.stomp-radius"),1,16,plugin.getConfig().getDouble(p+"attacks.stomp.radius",4.5)));
                in.add(range("giantquakeradius",plugin.getLanguage().get("giant-ui.quake-radius"),1,16,plugin.getConfig().getDouble(p+"attacks.quake.radius",8)));
                in.add(range("giantstompcd",plugin.getLanguage().get("giant-ui.stomp-cooldown"),8,1200,plugin.getConfig().getDouble(p+"attacks.stomp.cooldown-ticks",200)));
                in.add(range("giantquakecd",plugin.getLanguage().get("giant-ui.quake-cooldown"),8,1200,plugin.getConfig().getDouble(p+"attacks.quake.cooldown-ticks",360)));
                in.add(range("giantminionmax",plugin.getLanguage().get("giant-ui.minion-cap"),1,12,plugin.getConfig().getDouble(p+"reinforcements.max-active",3)));
            }
            case "alpha-spider" -> { in.add(range("cobweb",plugin.getLanguage().get("admindialogs.text-39"),0,100,plugin.getConfig().getDouble(p+"cobweb-chance",14.29))); in.add(range("minion",plugin.getLanguage().get("admindialogs.text-40"),0,100,plugin.getConfig().getDouble(p+"minion-chance",6.67))); }
            case "crying-bat" -> { in.add(range("cry",plugin.getLanguage().get("admindialogs.text-41"),0,100,plugin.getConfig().getDouble(p+"cry-chance",3.33))); in.add(range("death",plugin.getLanguage().get("admindialogs.text-42"),0,100,plugin.getConfig().getDouble(p+"death-chance",20))); }
            case "dead-miner" -> { in.add(bool("target","",plugin.getLanguage().get("admindialogs.text-43"),plugin.getConfig().getBoolean(p+"requires-target",true))); in.add(bool("torches","",plugin.getLanguage().get("admindialogs.text-44"),plugin.getConfig().getBoolean(p+"place-torches",true))); in.add(bool("redtorches","",plugin.getLanguage().get("admindialogs.text-45"),plugin.getConfig().getBoolean(p+"red-torches",false))); in.add(range("dropchance",plugin.getLanguage().get("admindialogs.text-46"),0,100,plugin.getConfig().getDouble(p+"drop-chance",30))); in.add(range("torchcooldown",plugin.getLanguage().get("admindialogs.text-47"),0,300,plugin.getConfig().getInt(p+"torches-cooldown",12))); in.add(DialogInput.text("headvalue",DialogText.component(plugin.getLanguage().get("admindialogs.text-48"))).initial(plugin.getConfig().getString(p+"head-value","")).maxLength(8192).build()); }
            case "watcher" -> in.add(DialogInput.text("headvalue",DialogText.component(plugin.getLanguage().get("admindialogs.text-49"))).initial(plugin.getConfig().getString(p+"head-value","")).maxLength(8192).build());
            case "hexed-armor" -> { in.add(bool("binding","",plugin.getLanguage().get("admindialogs.text-50"),plugin.getConfig().getBoolean(p+"binding-curse",true))); in.add(range("applychance",plugin.getLanguage().get("admindialogs.text-51"),0,100,plugin.getConfig().getDouble(p+"apply-chance",25))); }
            case "hungering-darkness" -> { in.add(bool("removeonlight","",plugin.getLanguage().get("admindialogs.text-52"),plugin.getConfig().getBoolean(p+"remove-on-light",false))); in.add(bool("deathsound","",plugin.getLanguage().get("admindialogs.text-53"),plugin.getConfig().getBoolean(p+"death-sound",true))); in.add(bool("nightvision","",plugin.getLanguage().get("admindialogs.text-54"),plugin.getConfig().getBoolean(p+"night-vision",false))); in.add(range("darkdamage",plugin.getLanguage().get("admindialogs.text-55"),0,200,plugin.getConfig().getDouble(p+"damage",200))); }
            case "lava-creeper" -> { in.add(range("firetouch",plugin.getLanguage().get("admindialogs.text-56"),0,200,plugin.getConfig().getInt(p+"fire-touch",10))); in.add(range("radius",plugin.getLanguage().get("admindialogs.text-57"),1,16,plugin.getConfig().getInt(p+"radius",4))); in.add(range("changechance",plugin.getLanguage().get("admindialogs.text-58"),0,100,plugin.getConfig().getDouble(p+"change-chance",50))); }
            case "smoke-demon" -> { in.add(range("maxlight",plugin.getLanguage().get("admindialogs.text-59"),0,15,plugin.getConfig().getInt(p+"max-light",11))); in.add(range("harmradius",plugin.getLanguage().get("admindialogs.text-60"),1,16,plugin.getConfig().getInt(p+"harm-radius",3))); in.add(DialogInput.text("particle",DialogText.component(plugin.getLanguage().get("admindialogs.text-61"))).initial(plugin.getConfig().getString(p+"particle","CLOUD")).maxLength(64).build()); }
            case "magma-monster" -> { in.add(bool("extdamage","",plugin.getLanguage().get("admindialogs.text-62"),plugin.getConfig().getBoolean(p+"extinguished-damage",false))); in.add(bool("target","",plugin.getLanguage().get("admindialogs.text-63"),plugin.getConfig().getBoolean(p+"requires-target",true))); in.add(range("firechance",plugin.getLanguage().get("admindialogs.text-64"),0,100,plugin.getConfig().getDouble(p+"fire-chance",7.14))); in.add(range("magmachance",plugin.getLanguage().get("admindialogs.text-65"),0,100,plugin.getConfig().getDouble(p+"magma-chance",3.57))); }
            case "tnt-creeper" -> { in.add(range("tntamount",plugin.getLanguage().get("admindialogs.text-66"),0,10,plugin.getConfig().getInt(p+"tnt-amount",2))); in.add(range("explosionchance",plugin.getLanguage().get("admindialogs.text-67"),0,100,plugin.getConfig().getDouble(p+"explosion-chance",33.33))); }
            case "chicken-jockey" -> in.add(range("speedamp",plugin.getLanguage().get("admindialogs.text-68"),0,10,plugin.getConfig().getInt(p+"speed-amplifier",3)));
            case "mimic" -> { in.add(range("chestdistance",plugin.getLanguage().get("admindialogs.text-69"),0,64,plugin.getConfig().getInt(p+"min-distance-from-chest",16))); in.add(range("maxchests",plugin.getLanguage().get("admindialogs.text-70"),0,20,plugin.getConfig().getInt(p+"max-active-chests",1))); in.add(range("lifetime",plugin.getLanguage().get("admindialogs.text-71"),0,3600,plugin.getConfig().getLong(p+"lifetime-seconds",900))); in.add(bool("removeunload","",plugin.getLanguage().get("admindialogs.text-72"),plugin.getConfig().getBoolean(p+"remove-on-unload",true))); }
            case "cave-golem" -> { in.add(bool("slowness","",plugin.getLanguage().get("admindialogs.text-73"),plugin.getConfig().getBoolean(p+"slowness",true))); in.add(bool("distract","",plugin.getLanguage().get("admindialogs.text-74"),plugin.getConfig().getBoolean(p+"distract-attack",true))); in.add(range("blockspawn",plugin.getLanguage().get("admindialogs.text-75"),0,100,plugin.getConfig().getDouble(p+"spawn-from-block",0.5))); in.add(range("damagemod",plugin.getLanguage().get("admindialogs.text-76"),0,10,plugin.getConfig().getDouble(p+"damage-modifier",2))); in.add(range("nonpick",plugin.getLanguage().get("admindialogs.text-77"),0,2,plugin.getConfig().getDouble(p+"nonpickaxe-modifier",0.07))); in.add(range("pick",plugin.getLanguage().get("admindialogs.text-78"),0,10,plugin.getConfig().getDouble(p+"pickaxe-modifier",2))); in.add(DialogInput.text("variants",DialogText.component(plugin.getLanguage().get("admindialogs.text-79"))).initial(String.join(", ",plugin.getConfig().getStringList(p+"variants"))).maxLength(2048).build()); }
            default -> { }
        }
    }

    private void saveSpecific(DialogResponseView v,String id,String p){
        switch(id){
            case "giant-zombie" -> {
                plugin.getConfig().set(p+"movement.enabled",yes(v.getBoolean("giantmove")));
                plugin.getConfig().set(p+"attacks.basic.enabled",yes(v.getBoolean("giantbasic")));
                plugin.getConfig().set(p+"attacks.stomp.enabled",yes(v.getBoolean("giantstomp")));
                plugin.getConfig().set(p+"attacks.quake.enabled",yes(v.getBoolean("giantquake")));
                plugin.getConfig().set(p+"reinforcements.enabled",yes(v.getBoolean("giantminions")));
                plugin.getConfig().set(p+"loot.enabled",yes(v.getBoolean("giantloot")));
                setNum(p+"movement.speed",v,"giantspeed");
                setNum(p+"attacks.basic.damage",v,"giantbasicdamage");
                setNum(p+"attacks.stomp.damage",v,"giantstompdamage");
                setNum(p+"attacks.quake.damage",v,"giantquakedamage");
                setNum(p+"attacks.stomp.radius",v,"giantstompradius");
                setNum(p+"attacks.quake.radius",v,"giantquakeradius");
                setNum(p+"attacks.stomp.cooldown-ticks",v,"giantstompcd");
                setNum(p+"attacks.quake.cooldown-ticks",v,"giantquakecd");
                setNum(p+"reinforcements.max-active",v,"giantminionmax");
            }
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
        in.add(bool("enabled","",plugin.getLanguage().get("admindialogs.text-80"),plugin.getConfig().getBoolean(p+"enabled",false)));
        in.add(range("ymin",plugin.getLanguage().get("admindialogs.text-81"),-64,320,plugin.getConfig().getInt(p+"y-min",-64))); in.add(range("ymax",plugin.getLanguage().get("admindialogs.text-82"),-64,320,plugin.getConfig().getInt(p+"y-max",320)));
        in.add(range("cooldown",plugin.getLanguage().get("admindialogs.text-83"),0,1800,plugin.getConfig().getLong(p+"cooldown-seconds",120))); in.add(range("intervalmin",plugin.getLanguage().get("admindialogs.text-84"),30,3600,plugin.getConfig().getLong(p+"encounter-interval.min-seconds",480))); in.add(range("intervalmax",plugin.getLanguage().get("admindialogs.text-85"),30,7200,plugin.getConfig().getLong(p+"encounter-interval.max-seconds",1200)));
        in.add(range("mindistance",plugin.getLanguage().get("admindialogs.text-86"),1,128,plugin.getConfig().getDouble(p+"min-distance-from-player",10))); in.add(range("maxdistance",plugin.getLanguage().get("admindialogs.text-87"),2,256,plugin.getConfig().getDouble(p+"max-distance-from-player",24)));
        in.add(bool("bossbar","",plugin.getLanguage().get("admindialogs.text-88"),plugin.getConfig().getBoolean(p+"combat.bossbar-enabled",false))); in.add(range("health",plugin.getLanguage().get("admindialogs.text-89"),1,200,plugin.getConfig().getDouble(p+"health",80))); in.add(range("damage",plugin.getLanguage().get("admindialogs.text-90"),0,20,plugin.getConfig().getDouble(p+"damage",2)));
        in.add(bool("sneaky","",plugin.getLanguage().get("admindialogs.text-91"),plugin.getConfig().getBoolean(p+"stalking.sneaky-strike.enabled",true))); in.add(range("sneakychance",plugin.getLanguage().get("admindialogs.text-92"),0,100,plugin.getConfig().getDouble(p+"stalking.sneaky-strike.chance",10)));
        in.add(bool("torches","",plugin.getLanguage().get("admindialogs.text-93"),plugin.getConfig().getBoolean(p+"redstone-torches.enabled",true))); in.add(range("torchchance",plugin.getLanguage().get("admindialogs.text-94"),0,100,plugin.getConfig().getDouble(p+"redstone-torches.chance",12)));
        in.add(bool("rewardcmd","",plugin.getLanguage().get("admindialogs.text-95"),plugin.getConfig().getBoolean(p+"reward.command.enabled",false))); in.add(DialogInput.text("rewardcommand",DialogText.component(plugin.getLanguage().get("admindialogs.text-96"))).initial(plugin.getConfig().getString(p+"reward.command.command","")).maxLength(1024).build());
        in.add(DialogInput.text("skin",DialogText.component(plugin.getLanguage().get("admindialogs.text-97"))).initial(plugin.getConfig().getString(p+"skin-value","")).maxLength(8192).build());
        Dialog d=Dialog.create(b->b.empty().base(DialogBase.builder(DialogText.component(plugin.getLanguage().get("admindialogs.text-98"))).body(List.of(DialogBody.item(new ItemStack(Material.PLAYER_HEAD),DialogBody.plainMessage(DialogText.component(plugin.getLanguage().get("admindialogs.text-99"))),true,true,48,48))).inputs(in).build()).type(DialogType.multiAction(List.of(
                button(plugin.getLanguage().get("admindialogs.text-100"),(v,a)->{if(!(a instanceof Player pl))return; plugin.getConfig().set(p+"enabled",yes(v.getBoolean("enabled"))); setNum(p+"y-min",v,"ymin");setNum(p+"y-max",v,"ymax");setNum(p+"cooldown-seconds",v,"cooldown");setNum(p+"encounter-interval.min-seconds",v,"intervalmin");setNum(p+"encounter-interval.max-seconds",v,"intervalmax");setNum(p+"min-distance-from-player",v,"mindistance");setNum(p+"max-distance-from-player",v,"maxdistance");plugin.getConfig().set(p+"combat.bossbar-enabled",yes(v.getBoolean("bossbar")));setNum(p+"health",v,"health");setNum(p+"damage",v,"damage");plugin.getConfig().set(p+"stalking.sneaky-strike.enabled",yes(v.getBoolean("sneaky")));setNum(p+"stalking.sneaky-strike.chance",v,"sneakychance");plugin.getConfig().set(p+"redstone-torches.enabled",yes(v.getBoolean("torches")));setNum(p+"redstone-torches.chance",v,"torchchance");plugin.getConfig().set(p+"reward.command.enabled",yes(v.getBoolean("rewardcmd")));plugin.getConfig().set(p+"reward.command.command",v.getText("rewardcommand"));plugin.getConfig().set(p+"skin-value",v.getText("skin"));saveReload();pl.sendMessage(DialogText.component(plugin.getLanguage().get("admindialogs.text-101")));}),
                button(plugin.getLanguage().get("admindialogs.text-102"),(v,a)->{if(a instanceof Player pl)plugin.getHerobrine().summonFor(pl,"stalking");}), button(plugin.getLanguage().get("admindialogs.text-103"),(v,a)->{if(a instanceof Player pl){plugin.getHerobrine().removeAll();pl.sendMessage(DialogText.component(plugin.getLanguage().get("admindialogs.text-104")));}}))).columns(3).build())); player.showDialog(d);
    }

    private DialogInput bool(String id,String key,String fallback,boolean value){return DialogInput.bool(id,DialogText.component(key.isEmpty()?fallback:ui.getString(key,fallback)),value,"true","false");}
    private DialogInput range(String id,String label,float min,float max,double value){return DialogInput.numberRange(id,DialogText.component(label),min,max).step(1f).initial((float)Math.max(min,Math.min(max,value))).build();}
    private void setNum(String path,DialogResponseView v,String id){Float f=v.getFloat(id);if(f!=null)plugin.getConfig().set(path,f.doubleValue());}
    private boolean yes(Boolean b){return Boolean.TRUE.equals(b);} private float orFloat(Float v,float f){return v==null?f:v;}
    private List<String> parseList(String raw){if(raw==null||raw.isBlank())return new ArrayList<>();List<String> out=new ArrayList<>();for(String x:raw.split("[,\\n]")){String v=x.trim().toUpperCase();if(!v.isEmpty())out.add(v);}return out;}
    private String configuredName(String id){String key="mobs."+id+".name"; String raw=plugin.getLanguage().displayValue("config",key,plugin.getConfig().getString(key,pretty(id)));return raw==null||raw.isBlank()?pretty(id):raw;}
    private String plainConfiguredName(String id){ return stripLegacyCodes(configuredName(id)); }
    private String stripLegacyCodes(String value){ return value == null ? "" : value.replaceAll("(?i)&[0-9A-FK-ORX]", ""); }
    private void saveReload(){plugin.saveConfig();plugin.reloadAll();}
    private ActionButton button(String label,DialogActionCallback cb){return ActionButton.builder(DialogText.component(label)).action(DialogAction.customClick(cb,oneUse())).build();}
    private ClickCallback.Options oneUse(){return ClickCallback.Options.builder().uses(1).build();}
    private String pretty(String id){StringBuilder o=new StringBuilder();for(String part:id.split("-"))o.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');return o.toString().trim();}
}
