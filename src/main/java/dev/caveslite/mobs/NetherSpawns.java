package dev.caveslite.mobs;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.BoundingBox;

/** Nether context checks; never searches unexplored terrain or creates entities to probe space. */
final class NetherSpawns {
    private NetherSpawns() {}

    static boolean excludedStructure(Location loc) {
        World world = loc.getWorld();
        for (Structure type : new Structure[]{Structure.FORTRESS, Structure.BASTION_REMNANT}) {
            for (var structure : world.getStructures(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, type)) {
                if (structure.getBoundingBox().contains(loc.toVector())) return true;
            }
        }
        return false;
    }

    static boolean nearRuinedPortal(Location loc, int radius) {
        World world = loc.getWorld();
        int minX = (loc.getBlockX() - radius) >> 4, maxX = (loc.getBlockX() + radius) >> 4;
        int minZ = (loc.getBlockZ() - radius) >> 4, maxZ = (loc.getBlockZ() + radius) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (!world.isChunkLoaded(x, z)) continue;
            for (var structure : world.getStructures(x, z, Structure.RUINED_PORTAL)) {
                BoundingBox box = structure.getBoundingBox();
                double dx = Math.max(box.getMinX() - loc.getX(), Math.max(0, loc.getX() - box.getMaxX()));
                double dy = Math.max(box.getMinY() - loc.getY(), Math.max(0, loc.getY() - box.getMaxY()));
                double dz = Math.max(box.getMinZ() - loc.getZ(), Math.max(0, loc.getZ() - box.getMaxZ()));
                if (dx * dx + dy * dy + dz * dz <= (double) radius * radius) return true;
            }
        }
        return false;
    }

    static boolean hasSpace(CustomMob mob, Location loc) {
        // Conservative envelopes include passengers and equipment-based encounters.
        double width = 1.0, height = 2.0;
        switch (mob.id()) {
            case "giant-zombie" -> { width = 3.6; height = 12; }
            case "cave-golem" -> { width = 1.4; height = 2.7; }
            case "alpha-spider", "spider-jockey" -> { width = 1.4; height = 3; }
            case "skeleton-horseman" -> { width = 1.4; height = 4; }
            case "chicken-jockey" -> { width = 1; height = 2.5; }
            case "magma-monster" -> { width = 2.1; height = 2.1; }
        }
        World world = loc.getWorld();
        BoundingBox body = new BoundingBox(loc.getX() - width / 2, loc.getY(), loc.getZ() - width / 2,
                loc.getX() + width / 2, loc.getY() + height, loc.getZ() + width / 2);
        if (body.getMinY() < world.getMinHeight() || body.getMaxY() > world.getMaxHeight()) return false;
        for (int x = (int)Math.floor(body.getMinX()); x < Math.ceil(body.getMaxX()); x++)
            for (int z = (int)Math.floor(body.getMinZ()); z < Math.ceil(body.getMaxZ()); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
                for (int y = (int)Math.floor(body.getMinY()); y < Math.ceil(body.getMaxY()); y++) {
                    var block = world.getBlockAt(x, y, z);
                    if (block.isLiquid() || block.getCollisionShape().overlaps(body.clone().shift(-x, -y, -z))) return false;
                }
            }
        return true;
    }
}
