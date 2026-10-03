package dev.caveslite;

import dev.caveslite.util.Locations;
import dev.caveslite.util.Rng;
import dev.caveslite.util.Sounds;
import dev.caveslite.util.WeightedPool;
import dev.caveslite.util.WorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/** Plays eerie sounds near players who are deep underground. */
public final class AmbientSounds {
    private record WrappedSound(Sound sound, float volume, float pitch) {}

    private final Plugin plugin;
    private WeightedPool<WrappedSound> sounds = new WeightedPool<>();
    private final WorldFilter worlds = new WorldFilter();

    private boolean enabled;
    private double chance;
    private int yMax;
    private double radius;
    private boolean serverWide;
    private double serverWideDistanceSquared;

    // Depth scaling: the deeper below yFull the player is, the more often
    // sounds play and the lower-pitched (creepier) they get.
    private boolean depthScaling;
    private int depthYFull;
    private int depthYNone;
    private double maxChanceMultiplier;
    private double pitchDrop;

    public AmbientSounds(Plugin plugin) {
        this.plugin = plugin;
    }

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        chance = cfg.getDouble("chance", 25) / 100;
        yMax = cfg.getInt("y-max", 64);
        radius = cfg.getDouble("near", 7);
        serverWide = cfg.getBoolean("server-wise", true);
        double distance = cfg.getDouble("server-wise-distance", 0);
        serverWideDistanceSquared = distance * distance;
        worlds.reload(cfg.getStringList("worlds"));

        ConfigurationSection depth = cfg.getConfigurationSection("depth-scaling");
        depthScaling = depth != null && depth.getBoolean("enabled", true);
        depthYFull = depth != null ? depth.getInt("y-full", -32) : -32;
        depthYNone = depth != null ? depth.getInt("y-none", 60) : 60;
        maxChanceMultiplier = depth != null ? depth.getDouble("max-chance-multiplier", 3.0) : 3.0;
        pitchDrop = depth != null ? depth.getDouble("pitch-drop", 0.3) : 0.3;

        sounds = new WeightedPool<>();
        ConfigurationSection soundsSection = cfg.getConfigurationSection("sounds");
        if (soundsSection != null) {
            for (String name : soundsSection.getKeys(false)) {
                Sound sound = Sounds.find(name);
                if (sound == null) {
                    plugin.getLogger().log(Level.WARNING, "Unknown ambient sound in config: {0}", name);
                    continue;
                }
                int weight = Math.max(1, soundsSection.getInt(name + ".weight", 10));
                sounds.add(new WrappedSound(
                        sound,
                        (float) soundsSection.getDouble(name + ".volume", 1),
                        (float) soundsSection.getDouble(name + ".pitch", 0.5)
                ), weight);
            }
        }
    }

    /** 0 at or above depthYNone, 1 at or below depthYFull, linear in between. */
    private double depthFactor(int y) {
        if (!depthScaling || depthYNone <= depthYFull) return 0;
        if (y <= depthYFull) return 1;
        if (y >= depthYNone) return 0;
        return (depthYNone - y) / (double) (depthYNone - depthYFull);
    }

    /** Called periodically by the plugin's scheduler. */
    public void tick() {
        if (!enabled || sounds.isEmpty()) return;

        // With "server-wise" the sound is heard by everyone nearby, so keep sources apart.
        List<Location> sources = serverWide && serverWideDistanceSquared > 0 ? new ArrayList<>() : null;

        for (World world : Bukkit.getWorlds()) {
            if (!worlds.allows(world)) continue;
            for (Player player : world.getPlayers()) {
                Location loc = player.getLocation();
                if (loc.getBlockY() > yMax || !Locations.isCave(loc)) continue;

                double depth = depthFactor(loc.getBlockY());
                double effectiveChance = Math.min(1, chance * (1 + (maxChanceMultiplier - 1) * depth));
                if (!Rng.chance(effectiveChance)) continue;

                if (sources != null) {
                    boolean tooClose = false;
                    for (Location source : sources) {
                        if (source.distanceSquared(loc) <= serverWideDistanceSquared) {
                            tooClose = true;
                            break;
                        }
                    }
                    if (tooClose) continue;
                    sources.add(loc);
                }

                play(sounds.next(), player, depth);
            }
        }
    }

    private void play(WrappedSound wrapped, Player player, double depth) {
        Location loc = player.getEyeLocation();
        if (radius > 0) {
            loc.add(Rng.nextDouble(-radius, radius), Rng.nextDouble(-radius, radius), Rng.nextDouble(-radius, radius));
        }
        float pitch = (float) Math.max(0.1, wrapped.pitch() - pitchDrop * depth);
        if (serverWide) {
            loc.getWorld().playSound(loc, wrapped.sound(), SoundCategory.AMBIENT, wrapped.volume(), pitch);
        } else {
            player.playSound(loc, wrapped.sound(), SoundCategory.AMBIENT, wrapped.volume(), pitch);
        }
    }
}
