package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobBase;
import dev.caveslite.mobs.MobManager;
import dev.caveslite.util.Locations;
import dev.caveslite.util.Materials;
import dev.caveslite.util.Rng;
import dev.caveslite.util.TagHelper;
import dev.caveslite.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns into a chest when it has no target and attacks whoever opens it.
 * Disabled by default (priority 0), like in the original plugin.
 */
public class Mimic extends MobBase implements CustomMob.Ticking, CustomMob.Cleanup, Listener {
    private static final PotionEffect BLINDNESS = new PotionEffect(PotionEffectType.BLINDNESS, 60, 1);

    private final MobManager manager;
    private final NamespacedKey chunkKey;
    private final NamespacedKey createdAtKey;

    private List<Material> items = new ArrayList<>();
    private boolean removeOnUnload;
    private boolean skipPersistenceCheck;
    private int minDistanceFromChest;
    private int maxActiveChests;
    private long lifetimeMillis;

    public Mimic(MobManager manager) {
        super(EntityType.WITHER_SKELETON, "mimic", 0, 30d);
        this.manager = manager;
        this.chunkKey = new NamespacedKey(manager.getPlugin(), "mimic-count");
        this.createdAtKey = new NamespacedKey(manager.getPlugin(), "mimic-created-at");
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        items = new ArrayList<>(Materials.getSet(cfg.getStringList("drop-items")));
        removeOnUnload = cfg.getBoolean("remove-on-unload", true);
        skipPersistenceCheck = cfg.getBoolean("skip-persistence-check", false);
        minDistanceFromChest = Math.max(0, cfg.getInt("min-distance-from-chest", 16));
        maxActiveChests = Math.max(0, cfg.getInt("max-active-chests", 1));
        lifetimeMillis = Math.max(0L, cfg.getLong("lifetime-seconds", 900L)) * 1000L;
    }

    @Override
    public boolean canSpawn(Location loc) {
        return (maxActiveChests <= 0 || countActiveChests(loc.getWorld()) < maxActiveChests)
                && !hasNearbyChest(loc, minDistanceFromChest);
    }

    @Override
    protected void prepare(LivingEntity entity) {
        entity.setSilent(true);
        entity.setCanPickupItems(false);

        EntityEquipment equipment = entity.getEquipment();
        equipment.setHelmet(new ItemStack(Material.CHEST));
        equipment.setItemInMainHand(new ItemStack(Material.SPRUCE_PLANKS));
        equipment.setItemInOffHand(new ItemStack(Material.SPRUCE_PLANKS));
        equipment.setChestplate(Materials.coloredLeather(Material.LEATHER_CHESTPLATE, 194, 105, 18));
        equipment.setLeggings(Materials.coloredLeather(Material.LEATHER_LEGGINGS, 194, 105, 18));
        equipment.setBoots(Materials.coloredLeather(Material.LEATHER_BOOTS, 194, 105, 18));
        equipment.setDropChance(EquipmentSlot.CHEST, 0f);
        equipment.setDropChance(EquipmentSlot.LEGS, 0f);
        equipment.setDropChance(EquipmentSlot.FEET, 0f);
    }

    // ------------------------------------------------------------ chest <-> mob

    @Override
    public void tick(LivingEntity entity) {
        Block block = entity.getLocation().getBlock();
        if (!(entity instanceof Monster monster) || monster.getTarget() != null || !block.getType().isAir()) return;

        for (BlockFace face : Locations.HORIZONTAL_FACES) {
            if (block.getRelative(face).getType() == Material.CHEST) return; // would make a double chest
        }
        if (maxActiveChests > 0 && countActiveChests(block.getWorld()) >= maxActiveChests) return;
        if (hasNearbyChest(block.getLocation(), minDistanceFromChest)) return;

        block.setType(Material.CHEST, false);
        Materials.rotate(block, Rng.randomElement(Locations.HORIZONTAL_FACES));
        TagHelper.setTag(block.getState(), "mimic-" + entity.getHealth());
        BlockState placedState = block.getState();
        if (placedState instanceof Chest placedChest) {
            placedChest.getPersistentDataContainer().set(createdAtKey, PersistentDataType.LONG, System.currentTimeMillis());
            placedChest.update(false, false);
        }
        entity.remove();

        if (removeOnUnload) {
            PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
            container.set(chunkKey, PersistentDataType.INTEGER,
                    container.getOrDefault(chunkKey, PersistentDataType.INTEGER, 0) + 1);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.CHEST && openMimic(block, event.getPlayer())) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    /** Stops players from attaching a second chest to a mimic. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) return;
        for (BlockFace face : Locations.HORIZONTAL_FACES) {
            Block relative = block.getRelative(face);
            if (relative.getType() == Material.CHEST && openMimic(relative, event.getPlayer())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean openMimic(Block block, Player player) {
        String tag = TagHelper.getTag(block.getState());
        if (tag == null || !tag.startsWith("mimic")) return false;
        if (!block.getRelative(BlockFace.UP).isPassable()) return true; // no room to pop out

        block.setType(Material.AIR);
        double savedHealth = Utils.getDouble(tag.substring(Math.min(6, tag.length())), health);
        if (savedHealth <= 0) savedHealth = 1;

        Location loc = block.getLocation();
        LivingEntity entity = manager.spawn(this, loc.clone().add(0.5, 0, 0.5));
        entity.setHealth(Math.min(savedHealth, health));
        Locations.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 0.5f);
        player.addPotionEffect(BLINDNESS);
        if (entity instanceof Monster monster) monster.setTarget(player);

        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        if (container.has(chunkKey, PersistentDataType.INTEGER)) {
            int amount = container.get(chunkKey, PersistentDataType.INTEGER);
            if (amount <= 1) {
                container.remove(chunkKey);
            } else {
                container.set(chunkKey, PersistentDataType.INTEGER, amount - 1);
            }
        }
        return true;
    }

    // -------------------------------------------------------- sounds and drops

    @EventHandler
    public void onDamaged(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (isThis(entity)) {
            Locations.playSound(entity.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1f, 0.2f);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isThis(event.getEntity())) return;
        Locations.playSound(event.getEntity().getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.HOSTILE, 1f, 0.2f);
        List<ItemStack> drops = event.getDrops();
        drops.clear();
        drops.add(new ItemStack(Material.CHEST));
        if (!items.isEmpty()) drops.add(new ItemStack(Rng.randomElement(items)));
    }

    private boolean hasNearbyChest(Location center, int radius) {
        if (radius <= 0) return false;
        int r2 = radius * radius;
        World world = center.getWorld();
        int minChunkX = (center.getBlockX() - radius) >> 4;
        int maxChunkX = (center.getBlockX() + radius) >> 4;
        int minChunkZ = (center.getBlockZ() - radius) >> 4;
        int maxChunkZ = (center.getBlockZ() + radius) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                for (BlockState tile : world.getChunkAt(cx, cz).getTileEntities()) {
                    if (!(tile instanceof Chest)) continue;
                    if (tile.getLocation().distanceSquared(center) <= r2) return true;
                }
            }
        }
        return false;
    }

    private int countActiveChests(World world) {
        int count = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            for (BlockState tile : chunk.getTileEntities()) {
                if (isMimicChest(tile)) count++;
            }
        }
        return count;
    }

    private boolean isMimicChest(BlockState state) {
        if (!(state instanceof Chest)) return false;
        String tag = TagHelper.getTag(state);
        return tag != null && tag.startsWith("mimic");
    }

    private boolean isExpired(BlockState state) {
        if (lifetimeMillis <= 0) return false;
        if (!(state instanceof Chest chest)) return false;
        Long created = chest.getPersistentDataContainer().get(createdAtKey, PersistentDataType.LONG);
        return created != null && System.currentTimeMillis() - created >= lifetimeMillis;
    }

    public int cleanupExpiredChests() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState tile : chunk.getTileEntities()) {
                    if (isMimicChest(tile) && isExpired(tile)) {
                        tile.getBlock().setType(Material.AIR, false);
                        removed++;
                    }
                }
            }
        }
        return removed;
    }

    @Override
    public int cleanupArtifacts() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState tile : chunk.getTileEntities()) {
                    if (isMimicChest(tile)) {
                        tile.getBlock().setType(Material.AIR, false);
                        removed++;
                    }
                }
                chunk.getPersistentDataContainer().remove(chunkKey);
            }
        }
        return removed;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoad(ChunkLoadEvent event) {
        for (BlockState tile : event.getChunk().getTileEntities()) {
            if (isMimicChest(tile) && isExpired(tile)) tile.getBlock().setType(Material.AIR, false);
        }
    }

    // ------------------------------------------------- cleanup of leftover chests

    /**
     * Optional: removes mimic chests when their chunk unloads, so they don't
     * pile up as fake chests in the world.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUnload(ChunkUnloadEvent event) {
        if (!removeOnUnload) return;
        PersistentDataContainer container = event.getChunk().getPersistentDataContainer();
        if (!skipPersistenceCheck && !container.has(chunkKey, PersistentDataType.INTEGER)) return;

        int remaining = skipPersistenceCheck ? Integer.MAX_VALUE : container.get(chunkKey, PersistentDataType.INTEGER);
        for (BlockState tile : event.getChunk().getTileEntities()) {
            if (remaining <= 0) break;
            if (!(tile instanceof Chest)) continue;
            String tag = TagHelper.getTag(tile);
            if (tag == null || !tag.startsWith("mimic")) continue;
            tile.getBlock().setType(Material.AIR, false);
            remaining--;
        }
        container.remove(chunkKey);
    }
}
