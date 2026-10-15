package dev.caveslite;

import dev.caveslite.lang.Lang;
import dev.caveslite.mobs.MobManager;
import dev.caveslite.util.TagHelper;
import dev.caveslite.util.Text;
import dev.caveslite.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /clite - CavesLite administration. */
public final class CavesCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("list", "summon", "herobrine", "kill", "kills", "options", "debug", "reload");

    private final CavesLite plugin;
    private final MobManager mobs;
    private final Lang lang;
    private final AdminDialogs dialogs;

    public CavesCommand(CavesLite plugin, MobManager mobs, Lang lang, AdminDialogs dialogs) {
        this.plugin = plugin;
        this.mobs = mobs;
        this.lang = lang;
        this.dialogs = dialogs;
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        Text.send(sender, lang.get(key, placeholders));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 && sender instanceof Player player) { dialogs.showMain(player); return true; }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> send(sender, "command.list", Map.of("mobs", String.join(", ", mobs.getMobIds())));

            case "options", "option", "opciones" -> {
                if (sender instanceof Player player) dialogs.showOptions(player);
                else Text.send(sender, plugin.getLanguage().get("cavescommand.text-1"));
            }

            case "reload", "r" -> {
                plugin.reloadAll();
                send(sender, "command.reload", null);
            }

            case "debug" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("mobs")) {
                    Text.send(sender, plugin.getLanguage().get("cavescommand.text-2") + label + " debug mobs [mob]");
                } else {
                    String filter = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : null;
                    if (filter != null && mobs.getMob(filter) == null) {
                        Text.send(sender, plugin.getLanguage().get("cavescommand.text-3") + filter);
                    } else {
                        List<String> lines = mobs.debugMobLines(filter);
                        Text.send(sender, plugin.getLanguage().get("cavescommand.text-4"));
                        for (String line : lines) Text.send(sender, "&7" + line);
                        Text.send(sender, plugin.getLanguage().get("cavescommand.text-5"));
                    }
                }
            }

            case "kill" -> {
                int removed;
                if (args.length < 2) {
                    removed = mobs.killAll(entity -> true);
                    removed += mobs.cleanupArtifacts(null);
                    plugin.getHerobrine().removeAll();
                } else {
                    String type = args[1].toLowerCase(Locale.ROOT);
                    if (type.equals("herobrine")) {
                        plugin.getHerobrine().removeAll();
                        removed = 0;
                    } else {
                        removed = mobs.killAll(entity -> TagHelper.isTagged(entity, type));
                        removed += mobs.cleanupArtifacts(type);
                    }
                }
                send(sender, "command.kill-removed", Map.of("amount", String.valueOf(removed)));
            }

            case "summon", "spawn" -> summon(sender, label, args);

            case "herobrine" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("debug")) {
                    Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : sender instanceof Player p ? p : null;
                    if (target == null) Text.send(sender, plugin.getLanguage().get("cavescommand.text-6") + label + lang.get("command.herobrine-debug-player"));
                    else Text.send(sender, "&7" + plugin.getHerobrine().debug(target));
                } else Text.send(sender, plugin.getLanguage().get("cavescommand.text-7") + label + lang.get("command.herobrine-debug"));
            }

            case "kills" -> {
                Player target = args.length >= 2 ? Bukkit.getPlayer(args[1])
                        : sender instanceof Player player ? player : null;
                if (target == null) {
                    send(sender, "command.kills-specify-player", Map.of("label", label));
                } else {
                    send(sender, "command.kills-result", Map.of(
                            "player", target.getName(),
                            "amount", String.valueOf(plugin.getAchievements().getKills(target))
                    ));
                }
            }

            default -> send(sender, "command.usage", Map.of("label", label));
        }
        return true;
    }

    private void summon(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            send(sender, "command.summon-specify-mob", Map.of("label", label));
            return;
        }
        String type = args[1].toLowerCase(Locale.ROOT);
        if (type.equals("herobrine")) {
            Player target = sender instanceof Player p ? p : null;
            String encounter = "stalking";
            if (args.length >= 3) {
                Player named = Bukkit.getPlayer(args[2]);
                if (named != null) {
                    target = named;
                    if (args.length >= 4) encounter = args[3];
                } else {
                    encounter = args[2];
                }
            }
            if (target == null) {
                Text.send(sender, plugin.getLanguage().get("cavescommand.text-8") + label + lang.get("command.herobrine-summon"));
                return;
            }
            if (!plugin.getHerobrine().summonFor(target, encounter)) {
                Text.send(sender, plugin.getLanguage().get("cavescommand.text-9"));
                return;
            }
            Text.send(sender, plugin.getLanguage().get("cavescommand.text-10") + target.getName() + "&a (" + encounter.toUpperCase(Locale.ROOT) + ").");
            return;
        }
        if (mobs.getMob(type) == null) {
            send(sender, "command.summon-unknown-mob", Map.of(
                    "mob", type, "mobs", String.join(", ", mobs.getMobIds())
            ));
            return;
        }

        Location loc = locationFrom(sender, Arrays.copyOfRange(args, 2, args.length));
        if (loc == null) {
            send(sender, "command.summon-specify-coords", Map.of("label", label, "mob", type));
            return;
        }
        mobs.spawn(type, loc);
        send(sender, "command.summon-success", Map.of("mob", type));
    }

    /** No arguments -> the player's own position; otherwise "x y z [world]". */
    private Location locationFrom(CommandSender sender, String[] coords) {
        if (coords.length == 0) {
            return sender instanceof Player player ? player.getLocation() : null;
        }
        if (coords.length < 3) return null;

        World world = coords.length >= 4 ? Bukkit.getWorld(coords[3])
                : sender instanceof Player player ? player.getWorld() : null;
        if (world == null) return null;

        double x = Utils.getDouble(coords[0], Double.NaN);
        double y = Utils.getDouble(coords[1], Double.NaN);
        double z = Utils.getDouble(coords[2], Double.NaN);
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) return null;
        return new Location(world, x, y, z);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBCOMMANDS);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            options.add("mobs");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("mobs")) {
            options.addAll(mobs.getMobIds());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("summon") || args[0].equalsIgnoreCase("kill"))) {
            options.addAll(mobs.getMobIds());
            if (args[0].equalsIgnoreCase("summon")) options.add("herobrine");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("summon") && args[1].equalsIgnoreCase("herobrine")) {
            options.addAll(List.of("lurking", "stalking", "creeping", "watcher"));
            for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("summon") && args[1].equalsIgnoreCase("herobrine")) {
            options.addAll(List.of("lurking", "stalking", "creeping", "watcher"));
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.startsWith(typed));
        return options;
    }
}
