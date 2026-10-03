package dev.caveslite;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobManager;
import dev.caveslite.util.Text;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
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

/** Native Dialog admin UI. Appearance lives in dialogs.yml; config.yml remains gameplay truth. */
public final class AdminDialogs {
    private final CavesLite plugin;
    private final MobManager mobs;
    private final Map<String, Material> icons = new LinkedHashMap<>();
    private YamlConfiguration ui;
    private Dialog cachedMain;

    public AdminDialogs(CavesLite plugin, MobManager mobs) {
        this.plugin = plugin;
        this.mobs = mobs;
        File file = new File(plugin.getDataFolder(), "dialogs.yml");
        if (!file.exists()) plugin.saveResource("dialogs.yml", false);
        reload();
    }

    public void reload() {
        ui = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "dialogs.yml"));
        icons.clear();
        for (String id : mobs.getMobIds()) {
            Material fallback = Material.SPAWNER;
            String raw = ui.getString("icons." + id, fallback.name());
            Material material = Material.matchMaterial(raw == null ? fallback.name() : raw);
            icons.put(id, material == null ? fallback : material);
        }
        cachedMain = buildMain();
    }

    public void showMain(Player player) { player.showDialog(cachedMain); }

    private Dialog buildMain() {
        List<ActionButton> actions = new ArrayList<>();
        String prefix = ui.getString("main.button-prefix", "&8[&c◆&8] &f");
        for (String id : mobs.getMobIds()) {
            actions.add(ActionButton.builder(Text.legacy(prefix + pretty(id)))
                    .tooltip(Text.legacy("&7" + id))
                    .action(DialogAction.customClick((view, audience) -> {
                        if (audience instanceof Player p) showMob(p, id);
                    }, ClickCallback.Options.builder().uses(1).build())).build());
        }
        actions.add(ActionButton.builder(Text.legacy(prefix + ui.getString("main.herobrine-label", "Herobrine")))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) showHerobrine(p);
                }, ClickCallback.Options.builder().uses(1).build())).build());
        int columns = Math.max(1, Math.min(10, ui.getInt("main.columns", 3)));
        return Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Text.legacy(ui.getString("main.title", "&4&lDangerousCaves")))
                        .body(mainBody())
                        .build())
                .type(DialogType.multiAction(actions).columns(columns).build()));
    }

    private List<DialogBody> mainBody() {
        List<DialogBody> body = new ArrayList<>();
        String raw = ui.getString("main.decorative-item", "SCULK_CATALYST");
        Material material = Material.matchMaterial(raw == null ? "SCULK_CATALYST" : raw);
        if (material != null) {
            body.add(DialogBody.item(new ItemStack(material),
                    DialogBody.plainMessage(Text.legacy(ui.getString("main.description", "&7Selecciona un encuentro."))),
                    true, true, 40, 40));
        } else {
            body.add(DialogBody.plainMessage(Text.legacy(ui.getString("main.description", "&7Selecciona un encuentro."))));
        }
        return body;
    }


    /** Global feature switches exposed through /dcaves options. */
    public void showOptions(Player player) {
        String ambientPath = plugin.getConfig().isConfigurationSection("caverns.ambient") ? "caverns.ambient.enabled" : "ambient.enabled";
        String footstepsPath = plugin.getConfig().isConfigurationSection("caverns.ambient") ? "caverns.ambient.footsteps.enabled" : "ambient.footsteps.enabled";

        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Text.legacy(ui.getString("options.title", "&4&lDangerousCaves &8- &fOpciones")))
                        .body(List.of(DialogBody.plainMessage(Text.legacy(ui.getString("options.description", "&7Activa o desactiva los sistemas globales del plugin.")))))
                        .inputs(List.of(
                                DialogInput.bool("ambient", Text.legacy(ui.getString("options.labels.ambient", "&bSonidos ambientales")), plugin.getConfig().getBoolean(ambientPath, true), "true", "false"),
                                DialogInput.bool("footsteps", Text.legacy(ui.getString("options.labels.footsteps", "&bPasos fantasma")), plugin.getConfig().getBoolean(footstepsPath, true), "true", "false"),
                                DialogInput.bool("tension", Text.legacy(ui.getString("options.labels.tension", "&cTension / latidos")), plugin.getConfig().getBoolean("tension.enabled", true), "true", "false"),
                                DialogInput.bool("bossbar", Text.legacy(ui.getString("options.labels.bossbar", "&dBossBars")), plugin.getConfig().getBoolean("boss-bar.enabled", false), "true", "false"),
                                DialogInput.bool("spawntitles", Text.legacy(ui.getString("options.labels.spawn-titles", "&eTitulos de aparicion")), plugin.getConfig().getBoolean("spawn-titles.enabled", true), "true", "false"),
                                DialogInput.bool("warnings", Text.legacy(ui.getString("options.labels.warnings", "&6Avisos ActionBar")), plugin.getConfig().getBoolean("warnings.enabled", true), "true", "false"),
                                DialogInput.bool("trails", Text.legacy(ui.getString("options.labels.trails", "&aTrails / particulas")), plugin.getConfig().getBoolean("trails.enabled", true), "true", "false"),
                                DialogInput.bool("achievements", Text.legacy(ui.getString("options.labels.achievements", "&6Achievements")), plugin.getConfig().getBoolean("achievements.enabled", true), "true", "false"),
                                DialogInput.bool("leaderboards", Text.legacy(ui.getString("options.labels.leaderboards", "&eLeaderboards")), plugin.getConfig().getBoolean("leaderboards.enabled", true), "true", "false")
                        )).build())
                .type(DialogType.multiAction(List.of(
                        button(ui.getString("options.buttons.save", "&a✔ Guardar"), (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            plugin.getConfig().set(ambientPath, Boolean.TRUE.equals(view.getBoolean("ambient")));
                            plugin.getConfig().set(footstepsPath, Boolean.TRUE.equals(view.getBoolean("footsteps")));
                            plugin.getConfig().set("tension.enabled", Boolean.TRUE.equals(view.getBoolean("tension")));
                            plugin.getConfig().set("boss-bar.enabled", Boolean.TRUE.equals(view.getBoolean("bossbar")));
                            plugin.getConfig().set("spawn-titles.enabled", Boolean.TRUE.equals(view.getBoolean("spawntitles")));
                            plugin.getConfig().set("warnings.enabled", Boolean.TRUE.equals(view.getBoolean("warnings")));
                            plugin.getConfig().set("trails.enabled", Boolean.TRUE.equals(view.getBoolean("trails")));
                            plugin.getConfig().set("achievements.enabled", Boolean.TRUE.equals(view.getBoolean("achievements")));
                            plugin.getConfig().set("leaderboards.enabled", Boolean.TRUE.equals(view.getBoolean("leaderboards")));
                            plugin.saveConfig();
                            plugin.reloadAll();
                            p.sendMessage(Text.legacy(ui.getString("options.saved", "&aOpciones globales actualizadas.")));
                        })
                )).columns(1).build()));
        player.showDialog(dialog);
    }

    private void showMob(Player player, String id) {
        CustomMob mob = mobs.getMob(id);
        if (mob == null) return;
        String path = "mobs." + id + ".";
        boolean enabled = plugin.getConfig().getBoolean(path + "enabled", false);
        int priority = plugin.getConfig().getInt(path + "priority", 0);
        boolean boss = plugin.getConfig().getBoolean("boss-bar.mobs." + id, false);
        String name = plugin.getConfig().getString(path + "name", pretty(id));
        boolean showName = plugin.getConfig().getBoolean(path + "show-name", false);
        int defaultMin = id.equals("giant-zombie") ? 80 : plugin.getConfig().getInt("mobs.y-min", -64);
        int defaultMax = id.equals("giant-zombie") ? 320 : plugin.getConfig().getInt("mobs.y-max", 64);
        int minY = plugin.getConfig().getInt(path + "spawn-y-min", defaultMin);
        int maxY = plugin.getConfig().getInt(path + "spawn-y-max", defaultMax);
        int cooldown = plugin.getConfig().getInt(path + "cooldown-seconds", 0);
        int maxActive = plugin.getConfig().getInt(path + "max-active", 0);
        ItemStack icon = new ItemStack(icons.getOrDefault(id, Material.SPAWNER));

        String label = "mob.labels.";
        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Text.legacy(ui.getString("mob.title-prefix", "&4&l") + pretty(id)))
                        .body(List.of(DialogBody.item(icon,
                                DialogBody.plainMessage(Text.legacy(ui.getString("mob.description", "&7Configuracion y prueba"))),
                                true, true, ui.getInt("mob.icon-width", 48), ui.getInt("mob.icon-height", 48))))
                        .inputs(List.of(
                                DialogInput.bool("enabled", Text.legacy(ui.getString(label + "enabled", "&aSpawn natural")), enabled, "true", "false"),
                                DialogInput.text("name", Text.legacy(ui.getString(label + "name", "&fNombre"))).initial(name == null ? "" : name).maxLength(128).build(),
                                DialogInput.bool("showname", Text.legacy(ui.getString(label + "show-name", "&fMostrar nombre")), showName, "true", "false"),
                                DialogInput.numberRange("priority", Text.legacy(ui.getString(label + "priority", "&ePrioridad")), 0, 20).step(1f).initial((float)Math.max(0, priority)).build(),
                                DialogInput.bool("bossbar", Text.legacy(ui.getString(label + "bossbar", "&dBossBar")), boss, "true", "false"),
                                DialogInput.numberRange("ymin", Text.legacy(ui.getString(label + "ymin", "&7Altura minima")), -64, 320).step(1f).initial((float)minY).build(),
                                DialogInput.numberRange("ymax", Text.legacy(ui.getString(label + "ymax", "&7Altura maxima")), -64, 320).step(1f).initial((float)maxY).build(),
                                DialogInput.numberRange("cooldown", Text.legacy(ui.getString(label + "cooldown", "&7Cooldown")), 0, 600).step(5f).initial((float)Math.min(600, cooldown)).build(),
                                DialogInput.numberRange("maxactive", Text.legacy(ui.getString(label + "maxactive", "&7Maximo activo")), 0, 20).step(1f).initial((float)Math.min(20, maxActive)).build()
                        )).build())
                .type(DialogType.multiAction(List.of(
                        button(ui.getString("mob.buttons.save", "&a✔ Guardar"), (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            int newPriority = Math.max(0, Math.round(orFloat(view.getFloat("priority"), priority)));
                            int newMinY = Math.round(orFloat(view.getFloat("ymin"), minY));
                            int newMaxY = Math.round(orFloat(view.getFloat("ymax"), maxY));
                            if (newMinY > newMaxY) { int swap = newMinY; newMinY = newMaxY; newMaxY = swap; }
                            plugin.getConfig().set(path + "enabled", Boolean.TRUE.equals(view.getBoolean("enabled")));
                            plugin.getConfig().set(path + "name", view.getText("name"));
                            plugin.getConfig().set(path + "show-name", Boolean.TRUE.equals(view.getBoolean("showname")));
                            plugin.getConfig().set(path + "priority", newPriority);
                            plugin.getConfig().set(path + "spawn-y-min", newMinY);
                            plugin.getConfig().set(path + "spawn-y-max", newMaxY);
                            plugin.getConfig().set(path + "cooldown-seconds", Math.max(0, Math.round(orFloat(view.getFloat("cooldown"), cooldown))));
                            plugin.getConfig().set(path + "max-active", Math.max(0, Math.round(orFloat(view.getFloat("maxactive"), maxActive))));
                            boolean newBoss = Boolean.TRUE.equals(view.getBoolean("bossbar"));
                            plugin.getConfig().set("boss-bar.mobs." + id, newBoss);
                            if (newBoss) plugin.getConfig().set("boss-bar.enabled", true);
                            plugin.saveConfig(); plugin.reloadAll();
                            p.sendMessage(Text.legacy("&aGuardado: &f" + pretty(id)));
                        }),
                        button(ui.getString("mob.buttons.summon", "&e⚡ Invocar"), (view, audience) -> {
                            if (audience instanceof Player p) mobs.spawn(id, p.getLocation());
                        }),
                        button(ui.getString("mob.buttons.remove", "&c✖ Eliminar"), (view, audience) -> {
                            if (audience instanceof Player p) {
                                int n = mobs.killAll(e -> dev.caveslite.util.TagHelper.isTagged(e, id));
                                n += mobs.cleanupArtifacts(id);
                                p.sendMessage(Text.legacy("&7Eliminados: &f" + n));
                            }
                        })
                )).columns(3).build()));
        player.showDialog(dialog);
    }

    private void showHerobrine(Player player) {
        boolean enabled = plugin.getConfig().getBoolean("herobrine.enabled", false);
        boolean boss = plugin.getConfig().getBoolean("herobrine.combat.bossbar-enabled", false);
        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Text.legacy("&4&lHerobrine"))
                        .body(List.of(DialogBody.item(new ItemStack(Material.PLAYER_HEAD), DialogBody.plainMessage(Text.legacy("&7Encuentro especial")), true, true, 48, 48)))
                        .inputs(List.of(
                                DialogInput.bool("enabled", Text.legacy("&aEncuentros naturales"), enabled, "true", "false"),
                                DialogInput.bool("bossbar", Text.legacy("&dBossBar de combate"), boss, "true", "false")
                        )).build())
                .type(DialogType.multiAction(List.of(
                        button(ui.getString("mob.buttons.save", "&a✔ Guardar"), (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            plugin.getConfig().set("herobrine.enabled", Boolean.TRUE.equals(view.getBoolean("enabled")));
                            plugin.getConfig().set("herobrine.combat.bossbar-enabled", Boolean.TRUE.equals(view.getBoolean("bossbar")));
                            plugin.saveConfig(); plugin.reloadAll(); p.sendMessage(Text.legacy("&aHerobrine actualizado."));
                        }),
                        button(ui.getString("mob.buttons.summon", "&e⚡ Invocar"), (view, audience) -> { if (audience instanceof Player p) plugin.getHerobrine().summonFor(p, "stalking"); }),
                        button(ui.getString("mob.buttons.remove", "&c✖ Eliminar"), (view, audience) -> { if (audience instanceof Player p) { plugin.getHerobrine().removeAll(); p.sendMessage(Text.legacy("&7Herobrine eliminado.")); } })
                )).columns(3).build()));
        player.showDialog(dialog);
    }

    private ActionButton button(String label, DialogActionCallback callback) {
        return ActionButton.builder(Text.legacy(label)).action(DialogAction.customClick(callback, ClickCallback.Options.builder().uses(1).build())).build();
    }

    private float orFloat(Float value, float fallback) {
        return value == null ? fallback : value.floatValue();
    }

    private String pretty(String id) {
        StringBuilder out = new StringBuilder();
        for (String part : id.split("-")) out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        return out.toString().trim();
    }
}
