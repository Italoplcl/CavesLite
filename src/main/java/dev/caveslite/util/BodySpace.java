package dev.caveslite.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

/** Collision checks for the entire body, not only the feet/head block. */
public final class BodySpace {
    private BodySpace() {}

    /** Paper PathResult points are the integer node origin, not a wide mob's center. */
    public static Location navigationCenter(Location node, double width) {
        double offset = (int)(width + 1) * 0.5;
        return node.clone().add(offset, 0, offset);
    }

    public static boolean fits(World world, BoundingBox box) {
        if (box.getMinY() < world.getMinHeight() || box.getMaxY() > world.getMaxHeight()) return false;
        for (int x = (int) Math.floor(box.getMinX()); x < Math.ceil(box.getMaxX()); x++) {
            for (int z = (int) Math.floor(box.getMinZ()); z < Math.ceil(box.getMaxZ()); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
                for (int y = Math.max(world.getMinHeight(), (int) Math.floor(box.getMinY()) - 1); y < Math.ceil(box.getMaxY()); y++) {
                    var block = world.getBlockAt(x, y, z);
                    BoundingBox local = box.clone().shift(-x, -y, -z);
                    if ((block.isLiquid() && new BoundingBox(0,0,0,1,1,1).overlaps(local))
                            || block.getCollisionShape().overlaps(local)) return false;
                }
            }
        }
        return true;
    }

    public static boolean fits(Location feet, double width, double height) {
        double half = width / 2 - 0.001;
        return fits(feet.getWorld(), new BoundingBox(feet.getX() - half, feet.getY() + 0.001,
                feet.getZ() - half, feet.getX() + half, feet.getY() + height - 0.001, feet.getZ() + half));
    }
}
