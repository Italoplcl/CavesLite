package dev.caveslite;

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

/** /dcaves [list|summon|kill|reload] */
public final class CavesCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("list", "summon", "kill", "kills", "reload");

    private final CavesLite plugin;
    private final MobManager mobs;

    public CavesCommand(CavesLite plugin, MobManager mobs) {
        this.plugin = plugin;
        this.mobs = mobs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> Text.send(sender, "&7Custom mobs: &f" + String.join(", ", mobs.getMobIds()));

            case "reload", "r" -> {
                plugin.reloadAll();
                Text.send(sender, "&aConfig reloaded.");
            }

            case "kill" -> {
                int removed;
                if (args.length < 2) {
                    removed = mobs.killAll(entity -> true);
                } else {
                    String type = args[1].toLowerCase(Locale.ROOT);
                    removed = mobs.killAll(entity -> TagHelper.isTagged(entity, type));
                }
                Text.send(sender, "&aRemoved &e" + removed + "&a custom mob(s).");
            }

            case "summon", "spawn" -> summon(sender, label, args);

            case "kills" -> {
                Player target = args.length >= 2 ? Bukkit.getPlayer(args[1])
                        : sender instanceof Player player ? player : null;
                if (target == null) {
                    Text.send(sender, "&cSpecify an online player: &f/" + label + " kills <player>");
                } else {
                    Text.send(sender, "&7" + target.getName() + "&7's custom mob kills: &e"
                            + plugin.getAchievements().getKills(target));
                }
            }

            default -> Text.send(sender, "&7Usage: &f/" + label + " <list|summon <mob> [x y z [world]]|kill [mob]|kills [player]|reload>");
        }
        return true;
    }

    private void summon(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            Text.send(sender, "&cSpecify a mob: &f/" + label + " summon <mob>");
            return;
        }
        String type = args[1].toLowerCase(Locale.ROOT);
        if (mobs.getMob(type) == null) {
            Text.send(sender, "&cUnknown mob &e" + type + "&c. Available: &f" + String.join(", ", mobs.getMobIds()));
            return;
        }

        Location loc = locationFrom(sender, Arrays.copyOfRange(args, 2, args.length));
        if (loc == null) {
            Text.send(sender, "&cGive coordinates: &f/" + label + " summon " + type + " <x> <y> <z> [world]");
            return;
        }
        mobs.spawn(type, loc);
        Text.send(sender, "&aSummoned &e" + type + "&a.");
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
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("summon") || args[0].equalsIgnoreCase("kill"))) {
            options.addAll(mobs.getMobIds());
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.startsWith(typed));
        return options;
    }
}
