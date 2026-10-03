package dev.caveslite;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Native Minecraft Dialog admin UI. YAML remains the persistent source of truth. */
public final class AdminDialogs {
    private final CavesLite plugin;
    private final MobManager mobs;
    private final Map<String, Material> icons = new LinkedHashMap<>();

    public AdminDialogs(CavesLite plugin, MobManager mobs) {
        this.plugin = plugin; this.mobs = mobs;
        icons.put("alpha-spider", Material.SPIDER_EYE); icons.put("cave-golem", Material.IRON_PICKAXE);
        icons.put("crying-bat", Material.BAT_SPAWN_EGG); icons.put("dead-miner", Material.PLAYER_HEAD);
        icons.put("hexed-armor", Material.IRON_CHESTPLATE); icons.put("hungering-darkness", Material.BLACK_DYE);
        icons.put("lava-creeper", Material.LAVA_BUCKET); icons.put("magma-monster", Material.MAGMA_BLOCK);
        icons.put("mimic", Material.CHEST); icons.put("smoke-demon", Material.CAMPFIRE);
        icons.put("tnt-creeper", Material.TNT); icons.put("watcher", Material.ENDER_EYE);
        icons.put("giant-zombie", Material.ZOMBIE_HEAD); icons.put("chicken-jockey", Material.CHICKEN_SPAWN_EGG);
        icons.put("killer-bunny", Material.RABBIT_FOOT); icons.put("illusioner", Material.BOW);
        icons.put("spider-jockey", Material.SKELETON_SKULL); icons.put("skeleton-horseman", Material.SKELETON_HORSE_SPAWN_EGG);
    }

    public void showMain(Player player) {
        List<ActionButton> actions = new ArrayList<>();
        for (String id : mobs.getMobIds()) {
            Material icon = icons.getOrDefault(id, Material.SPAWNER);
            actions.add(ActionButton.builder(Component.text(pretty(id)))
                    .tooltip(Component.text(icon.name().toLowerCase().replace('_', ' ')))
                    .action(DialogAction.customClick((view, audience) -> {
                        if (audience instanceof Player p) showMob(p, id);
                    }, ClickCallback.Options.builder().uses(1).build())).build());
        }
        actions.add(ActionButton.builder(Component.text("Herobrine"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) showHerobrine(p);
                }, ClickCallback.Options.builder().uses(1).build())).build());
        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Component.text("DangerousCaves — Administración"))
                        .body(List.of(DialogBody.plainMessage(Component.text("Selecciona una criatura o encuentro."))))
                        .build())
                .type(DialogType.multiAction(actions).columns(3).build()));
        player.showDialog(dialog);
    }

    private void showMob(Player player, String id) {
        CustomMob mob = mobs.getMob(id);
        if (mob == null) return;
        int priority = plugin.getConfig().getInt("mobs." + id + ".priority", 0);
        boolean boss = plugin.getConfig().getBoolean("boss-bar.mobs." + id, false);
        int defaultMin = id.equals("giant-zombie") ? 80 : plugin.getConfig().getInt("mobs.y-min", -64);
        int defaultMax = id.equals("giant-zombie") ? 320 : plugin.getConfig().getInt("mobs.y-max", 64);
        int minY = plugin.getConfig().getInt("mobs." + id + ".spawn-y-min", defaultMin);
        int maxY = plugin.getConfig().getInt("mobs." + id + ".spawn-y-max", defaultMax);
        int cooldown = plugin.getConfig().getInt("mobs." + id + ".cooldown-seconds", 0);
        int maxActive = plugin.getConfig().getInt("mobs." + id + ".max-active", 0);
        ItemStack icon = new ItemStack(icons.getOrDefault(id, Material.SPAWNER));
        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Component.text(pretty(id)))
                        .body(List.of(DialogBody.item(icon, DialogBody.plainMessage(Component.text("Configuración y prueba")), true, true, 48, 48)))
                        .inputs(List.of(
                                DialogInput.bool("enabled", Component.text("Spawn natural"), priority > 0, "true", "false"),
                                DialogInput.numberRange("priority", Component.text("Prioridad"), 1, 20).step(1f).initial((float)Math.max(1, priority)).build(),
                                DialogInput.bool("bossbar", Component.text("BossBar"), boss, "true", "false"),
                                DialogInput.numberRange("ymin", Component.text("Altura mínima"), -64, 320).step(1f).initial((float)minY).build(),
                                DialogInput.numberRange("ymax", Component.text("Altura máxima"), -64, 320).step(1f).initial((float)maxY).build(),
                                DialogInput.numberRange("cooldown", Component.text("Cooldown (segundos)"), 0, 600).step(5f).initial((float)Math.min(600, cooldown)).build(),
                                DialogInput.numberRange("maxactive", Component.text("Máximo activo (0 = sin límite)"), 0, 20).step(1f).initial((float)Math.min(20, maxActive)).build()
                        )).build())
                .type(DialogType.multiAction(List.of(
                        button("Guardar", (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            boolean enabled = Boolean.TRUE.equals(view.getBoolean("enabled"));
                            int newPriority = Math.max(1, Math.round(view.getFloat("priority")));
                            boolean newBoss = Boolean.TRUE.equals(view.getBoolean("bossbar"));
                            int newMinY = Math.round(view.getFloat("ymin"));
                            int newMaxY = Math.round(view.getFloat("ymax"));
                            if (newMinY > newMaxY) { int swap = newMinY; newMinY = newMaxY; newMaxY = swap; }
                            int newCooldown = Math.max(0, Math.round(view.getFloat("cooldown")));
                            int newMaxActive = Math.max(0, Math.round(view.getFloat("maxactive")));
                            plugin.getConfig().set("mobs." + id + ".priority", enabled ? newPriority : 0);
                            plugin.getConfig().set("mobs." + id + ".spawn-y-min", newMinY);
                            plugin.getConfig().set("mobs." + id + ".spawn-y-max", newMaxY);
                            plugin.getConfig().set("mobs." + id + ".cooldown-seconds", newCooldown);
                            plugin.getConfig().set("mobs." + id + ".max-active", newMaxActive);
                            plugin.getConfig().set("boss-bar.mobs." + id, newBoss);
                            if (newBoss) plugin.getConfig().set("boss-bar.enabled", true);
                            plugin.saveConfig(); plugin.reloadAll();
                            p.sendMessage(Component.text("Guardado: " + pretty(id)));
                        }),
                        button("Invocar", (view, audience) -> {
                            if (audience instanceof Player p) mobs.spawn(id, p.getLocation());
                        }),
                        button("Eliminar", (view, audience) -> {
                            if (audience instanceof Player p) {
                                int n = mobs.killAll(e -> dev.caveslite.util.TagHelper.isTagged(e, id));
                                n += mobs.cleanupArtifacts(id);
                                p.sendMessage(Component.text("Eliminados: " + n));
                            }
                        })
                )).columns(3).build()));
        player.showDialog(dialog);
    }

    private void showHerobrine(Player player) {
        boolean enabled = plugin.getConfig().getBoolean("herobrine.enabled", false);
        boolean boss = plugin.getConfig().getBoolean("herobrine.combat.bossbar-enabled", false);
        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Component.text("Herobrine"))
                        .body(List.of(DialogBody.item(new ItemStack(Material.PLAYER_HEAD), DialogBody.plainMessage(Component.text("Encuentro especial")), true, true, 48, 48)))
                        .inputs(List.of(
                                DialogInput.bool("enabled", Component.text("Encuentros naturales"), enabled, "true", "false"),
                                DialogInput.bool("bossbar", Component.text("BossBar de combate"), boss, "true", "false")
                        )).build())
                .type(DialogType.multiAction(List.of(
                        button("Guardar", (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            plugin.getConfig().set("herobrine.enabled", Boolean.TRUE.equals(view.getBoolean("enabled")));
                            plugin.getConfig().set("herobrine.combat.bossbar-enabled", Boolean.TRUE.equals(view.getBoolean("bossbar")));
                            plugin.saveConfig(); plugin.reloadAll(); p.sendMessage(Component.text("Herobrine actualizado."));
                        }),
                        button("Invocar", (view, audience) -> { if (audience instanceof Player p) plugin.getHerobrine().summonFor(p, "stalking"); }),
                        button("Eliminar", (view, audience) -> { if (audience instanceof Player p) { plugin.getHerobrine().removeAll(); p.sendMessage(Component.text("Herobrine eliminado.")); } })
                )).columns(3).build()));
        player.showDialog(dialog);
    }

    private ActionButton button(String label, io.papermc.paper.registry.data.dialog.action.DialogActionCallback callback) {
        return ActionButton.builder(Component.text(label)).action(DialogAction.customClick(callback, ClickCallback.Options.builder().uses(1).build())).build();
    }

    private String pretty(String id) {
        StringBuilder out = new StringBuilder();
        for (String part : id.split("-")) out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        return out.toString().trim();
    }
}
