package dev.caveslite.util;

import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

/** Marks entities and blocks owned by CavesLite. */
public final class TagHelper {
    public static final NamespacedKey MOB_KEY = Objects.requireNonNull(NamespacedKey.fromString("caveslite:mob-type"));
    public static final String SCOREBOARD_TAG = "caveslite-mob";
    public static final NamespacedKey SPAWN_TIME_KEY = Objects.requireNonNull(NamespacedKey.fromString("caveslite:spawn-time"));

    private TagHelper() {}

    public static void setTag(LivingEntity entity, String tag) {
        entity.getPersistentDataContainer().set(MOB_KEY, PersistentDataType.STRING, tag);
        entity.addScoreboardTag(SCOREBOARD_TAG);
        entity.addScoreboardTag(mobScoreboardTag(tag));
        if (!entity.getPersistentDataContainer().has(SPAWN_TIME_KEY, PersistentDataType.LONG)) {
            entity.getPersistentDataContainer().set(SPAWN_TIME_KEY, PersistentDataType.LONG, System.currentTimeMillis());
        }
    }

    public static void setTag(BlockState state, String tag) {
        if (state instanceof PersistentDataHolder holder) {
            holder.getPersistentDataContainer().set(MOB_KEY, PersistentDataType.STRING, tag);
            state.update(false, false);
        }
    }

    public static String getTag(LivingEntity entity) {
        return entity.getPersistentDataContainer().get(MOB_KEY, PersistentDataType.STRING);
    }

    public static String getTag(BlockState state) {
        if (state instanceof PersistentDataHolder holder) {
            return holder.getPersistentDataContainer().get(MOB_KEY, PersistentDataType.STRING);
        }
        return null;
    }

    public static boolean isTagged(LivingEntity entity) {
        return entity.getPersistentDataContainer().has(MOB_KEY, PersistentDataType.STRING);
    }

    public static boolean isTagged(LivingEntity entity, String tag) {
        return tag.equals(getTag(entity));
    }

    public static long getSpawnTime(LivingEntity entity) {
        Long value = entity.getPersistentDataContainer().get(SPAWN_TIME_KEY, PersistentDataType.LONG);
        return value == null ? 0L : value;
    }

    public static String mobScoreboardTag(String id) {
        return SCOREBOARD_TAG + "-" + id;
    }
}
