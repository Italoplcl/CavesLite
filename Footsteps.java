package dev.caveslite;

import dev.caveslite.util.Locations;
import dev.caveslite.util.Rng;
import dev.caveslite.util.Sounds;
import dev.caveslite.util.WorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.logging.Level;

/**
 * Ghost footsteps: plays a step sound a few blocks behind (or around) a
 * lone player underground, with no mob actually there.
 */
public final class Footsteps {
    private final Plugin plugin;
    private final WorldFilter worlds = new WorldFilter();

    private boolean enabled;
    private double chance;
    private int yMax;
    private double distance;
    private boolean behindOnly;
    private boolean requireAlone;
    private Sound sound;
    private float volume;
    private float pitch;

    public Footsteps(Plugin plugin) {
        this.plugin = plugin;
    }

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        chance = cfg.getDouble("chance", 8) / 100;
        yMax = cfg.getInt("y-max", 40);
        distance = cfg.getDouble("distance", 4);
        behindOnly = cfg.getBoolean("behind-only", true);
        requireAlone = cfg.getBoolean("require-alone", true);
        volume = (float) cfg.getDouble("volume", 0.6);
        pitch = (float) cfg.getDouble("pitch", 0.75);

        String name = cfg.getString("sound", "minecraft:block.stone.step");
        sound = Sounds.find(name);
        if (sound == null) {
            plugin.getLogger().log(Level.WARNING, "Unknown footsteps sound in config: {0}", name);
        }
        worlds.reload(cfg.getStringList("worlds"));
    }

    /** Called periodically by the plugin's scheduler. */
    public void tick() {
        if (!enabled || sound == null) return;

        for (World world : Bukkit.getWorlds()) {
            if (!worlds.allows(world)) continue;
            for (Player player : world.getPlayers()) {
                Location loc = player.getLocation();
                if (loc.getBlockY() > yMax || !Locations.isCave(loc) || !Rng.chance(chance)) continue;
                if (requireAlone && hasNearbyPlayer(player)) continue;

                play(player);
            }
        }
    }

    private boolean hasNearbyPlayer(Player player) {
        double radiusSquared = 20 * 20;
        for (Player other : player.getWorld().getPlayers()) {
            if (other != player && other.getLocation().distanceSquared(player.getLocation()) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private void play(Player player) {
        Location loc = player.getLocation();
        if (behindOnly) {
            Vector back = loc.getDirection().setY(0).normalize().multiply(-distance);
            loc.add(back);
        } else {
            loc.add(Rng.nextDouble(-distance, distance), 0, Rng.nextDouble(-distance, distance));
        }
        player.playSound(loc, sound, SoundCategory.AMBIENT, volume, pitch + (float) Rng.nextDouble(-0.05, 0.05));
    }
}
