package dev.caveslite.mobs;

import dev.caveslite.util.Locations;
import dev.caveslite.util.Rng;
import dev.caveslite.util.TagHelper;
import dev.caveslite.util.Utils;
import dev.caveslite.util.WeightedPool;
import dev.caveslite.util.WorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Registers custom mobs, replaces natural spawns with them and ticks the ones that need it. */
public final class MobManager implements Listener {
    /** Notified right after a custom mob is actually spawned into the world. */
    public interface SpawnListener {
        void onSpawn(CustomMob mob, LivingEntity entity);
    }

    private final Plugin plugin;
    private final Map<String, CustomMob> mobs = new LinkedHashMap<>();
    private final List<SpawnListener> spawnListeners = new java.util.ArrayList<>();
    private final Map<CustomMob.Ticking, Set<UUID>> tracked = new HashMap<>();
    private final Map<String, Set<UUID>> activeByMob = new HashMap<>();
    private final WorldFilter worlds = new WorldFilter();

    private WeightedPool<CustomMob> pool = new WeightedPool<>();
    private Set<EntityType> replaceTypes = Set.of();
    private boolean enabled;
    private double chance;
    private int yMin;
    private int yMax;
    private int maxLight;
    private boolean blockRename;
    private final Map<String, Long> lastNaturalSpawn = new HashMap<>();

    public MobManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(CustomMob mob) {
        mobs.put(mob.id(), mob);
        activeByMob.put(mob.id(), new HashSet<>());
        if (mob instanceof Listener listener) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
        if (mob instanceof CustomMob.Ticking ticking) {
            tracked.put(ticking, new HashSet<>());
        }
    }

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        chance = cfg.getDouble("try-chance", 50) / 100;
        yMin = cfg.getInt("y-min", -64);
        yMax = cfg.getInt("y-max", 64);
        maxLight = cfg.getInt("max-light-level", 16);
        blockRename = cfg.getBoolean("restrict-rename", false);
        worlds.reload(cfg.getStringList("worlds"));

        List<String> replace = cfg.isList("replace-mobs")
                ? cfg.getStringList("replace-mobs")
                : Arrays.asList("ZOMBIE", "HUSK", "SKELETON", "STRAY", "CREEPER", "SPIDER", "WITCH", "ENDERMAN");
        replaceTypes = Utils.entityTypes(replace);

        pool = new WeightedPool<>();
        for (CustomMob mob : mobs.values()) {
            mob.reload(Utils.section(cfg, mob.id()));
            if (mob.enabled() && mob.weight() > 0) pool.add(mob, mob.weight());
        }
    }

    // ---------------------------------------------------------------- spawning

    public CustomMob getMob(String id) {
        return mobs.get(id);
    }

    public Set<String> getMobIds() {
        return mobs.keySet();
    }

    public boolean isWorldEnabled(World world) {
        return worlds.allows(world);
    }

    public LivingEntity spawn(String id, Location loc) {
        CustomMob mob = mobs.get(id);
        return mob == null ? null : spawn(mob, loc);
    }

    public void addSpawnListener(SpawnListener listener) {
        spawnListeners.add(listener);
    }

    public LivingEntity spawn(CustomMob mob, Location loc) {
        LivingEntity entity = mob.spawn(loc);
        activeByMob.get(mob.id()).add(entity.getUniqueId());
        if (mob instanceof CustomMob.Ticking ticking) {
            tracked.get(ticking).add(entity.getUniqueId());
        }
        entity.setRemoveWhenFarAway(true);
        for (SpawnListener listener : spawnListeners) {
            listener.onSpawn(mob, entity);
        }
        return entity;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (!enabled || pool.isEmpty() || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (!replaceTypes.contains(event.getEntityType())) return;

        Location loc = event.getLocation();
        if (!worlds.allows(loc.getWorld())
                || (maxLight < 16 && loc.getBlock().getLightLevel() > Math.max(0, maxLight))
                || !Rng.chance(chance)) {
            return;
        }

        CustomMob mob = pool.next();
        if ((mob.usesGlobalYRange() && (loc.getBlockY() > yMax || loc.getBlockY() < yMin))
                || loc.getBlockY() < mob.spawnYMin() || loc.getBlockY() > mob.spawnYMax()
                || !mob.naturalContextAllowed(loc) || !mob.canSpawn(loc)) return;
        long now = System.currentTimeMillis();
        if (mob.cooldownMillis() > 0 && now - lastNaturalSpawn.getOrDefault(mob.id(), 0L) < mob.cooldownMillis()) return;
        if (mob.maxActive() > 0 && countActive(mob.id()) >= mob.maxActive()) return;

        event.setCancelled(true);
        spawn(mob, loc);
        lastNaturalSpawn.put(mob.id(), now);
    }


    private int countActive(String id) {
        Set<UUID> ids = activeByMob.get(id);
        if (ids == null) return 0;
        ids.removeIf(uuid -> { Entity e = Bukkit.getEntity(uuid); return e == null || !e.isValid(); });
        return ids.size();
    }

    // ----------------------------------------------------------------- ticking

    /** Called every few ticks by the plugin's scheduler. */
    public void tick() {
        for (Map.Entry<CustomMob.Ticking, Set<UUID>> entry : tracked.entrySet()) {
            CustomMob.Ticking mob = entry.getKey();
            Iterator<UUID> uuids = entry.getValue().iterator();
            while (uuids.hasNext()) {
                Entity entity = Bukkit.getEntity(uuids.next());
                if (!(entity instanceof LivingEntity living) || !living.isValid()) {
                    uuids.remove(); // dead, removed or in an unloaded chunk (re-added on load)
                } else {
                    mob.tick(living);
                }
            }
        }
    }

    /** Mobs come back from disk with their tag; start ticking them again. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            String id = TagHelper.getTag(living);
            if (id != null && activeByMob.containsKey(id)) activeByMob.get(id).add(living.getUniqueId());
            if (id != null && mobs.get(id) instanceof CustomMob.Ticking ticking) {
                tracked.get(ticking).add(living.getUniqueId());
            }
        }
    }

    // -------------------------------------------------------------------- misc

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!blockRename) return;
        Material item = event.getHand() == EquipmentSlot.HAND
                ? event.getPlayer().getInventory().getItemInMainHand().getType()
                : event.getPlayer().getInventory().getItemInOffHand().getType();
        if (item == Material.NAME_TAG && event.getRightClicked() instanceof LivingEntity living
                && TagHelper.isTagged(living)) {
            event.setCancelled(true);
        }
    }

    /** Removes every custom mob (or only the ones passing the filter). */
    public int killAll(java.util.function.Predicate<LivingEntity> filter) {
        int[] count = {0};
        Consumer<LivingEntity> remover = entity -> {
            entity.remove();
            count[0]++;
        };
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                String id = TagHelper.getTag(entity);
                if (id != null && mobs.containsKey(id) && filter.test(entity)) remover.accept(entity);
            }
        }
        return count[0];
    }

    /** Removes temporary blocks/artifacts created by custom mobs. */
    public int cleanupArtifacts(String mobId) {
        int removed = 0;
        if (mobId == null) {
            for (CustomMob mob : mobs.values()) {
                if (mob instanceof CustomMob.Cleanup cleanup) removed += cleanup.cleanupArtifacts();
            }
        } else {
            CustomMob mob = mobs.get(mobId);
            if (mob instanceof CustomMob.Cleanup cleanup) removed += cleanup.cleanupArtifacts();
        }
        return removed;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
