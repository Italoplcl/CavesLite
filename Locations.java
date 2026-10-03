package dev.caveslite.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Consumer;

public final class Locations {
    public static final List<BlockFace> HORIZONTAL_FACES =
            List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    private Locations() {}

    public static void loop(int radius, Location start, Consumer<Location> consumer) {
        loop(radius, start, (world, x, y, z) -> consumer.accept(new Location(world, x, y, z)));
    }

    public static void loop(int radius, Location start, BlockConsumer consumer) {
        World world = start.getWorld();
        int sx = start.getBlockX(), sy = start.getBlockY(), sz = start.getBlockZ();
        for (int x = sx - radius; x <= sx + radius; x++) {
            for (int y = sy - radius; y <= sy + radius; y++) {
                for (int z = sz - radius; z <= sz + radius; z++) {
                    consumer.accept(world, x, y, z);
                }
            }
        }
    }

    /** Roughly "underground and enclosed": little sky light and cave-like blocks below. */
    public static boolean isCave(Location loc) {
        Block block = loc.getBlock();
        if (block.getLightFromSky() > 1) return false;
        Material below = block.getRelative(BlockFace.DOWN).getType();
        return Materials.isCave(below) || below.isAir();
    }

    public static boolean isLookingAt(LivingEntity viewer, LivingEntity target) {
        Location eye = viewer.getEyeLocation();
        Vector toEntity = target.getEyeLocation().toVector().subtract(eye.toVector());
        return toEntity.normalize().dot(eye.getDirection()) > 0.70D;
    }

    public static void playSound(Location loc, Sound sound, SoundCategory category, float volume, float pitch) {
        loc.getWorld().playSound(loc, sound, category, volume, pitch);
    }

    public static void playSound(Location loc, Sound sound, float volume, float pitch) {
        loc.getWorld().playSound(loc, sound, volume, pitch);
    }

    @FunctionalInterface
    public interface BlockConsumer {
        void accept(World world, int x, int y, int z);
    }
}
