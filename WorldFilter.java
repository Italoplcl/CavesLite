package dev.caveslite.util;

import org.bukkit.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Worlds where a feature is active. An empty list in the config means
 * "every normal (overworld-type) world".
 */
public final class WorldFilter {
    private final Set<String> names = new HashSet<>();

    public void reload(List<String> configured) {
        names.clear();
        names.addAll(configured);
    }

    public boolean allows(World world) {
        if (names.isEmpty()) {
            return world.getEnvironment() == World.Environment.NORMAL;
        }
        return names.contains(world.getName());
    }
}
