package dev.caveslite.herobrine;

import dev.caveslite.lang.Lang;
import dev.caveslite.util.Locations;
import dev.caveslite.util.Rng;
import dev.caveslite.util.Sounds;
import dev.caveslite.util.Text;
import dev.caveslite.util.WorldFilter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Mannequin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Herobrine: a psychological-horror encounter, not a boss. The design goal
 * (from the spec) is that the player should end up thinking "did I just see
 * something?", not "I need to kill that". Most of the time nothing happens
 * beyond being briefly watched or hearing something that might have been
 * nothing. Combat is the exception, triggered only if a player attacks it.
 *
 * Deliberately kept OUT of the shared mob systems (TagHelper tagging, and so
 * boss-bar/warnings/trails/tension/leaderboards/achievements): those exist
 * to advertise a mob's presence, which is the opposite of what this is for.
 * Herobrine has its own encounter-only boss bar, shown solely during combat.
 *
 * Simplifications from the spec, called out explicitly rather than silently:
 * - "Protected zones" (Trial Chambers, Ancient Cities) are avoided with a
 *   lightweight material check near the candidate spot (vault/trial-spawner/
 *   sculk-catalyst blocks), not a real structure lookup - that API
 *   (World#locateNearestStructure) is too expensive to call on every spawn
 *   attempt.
 * - "Imita sonidos del jugador" is simplified to a plain delayed echo of a
 *   configured sound, not literally re-playing whatever sound the player's
 *   last action made.
 * - "Sin barra de vida visible" is implemented as a boss bar whose progress
 *   never reflects real HP (always full) - Adventure's BossBar always shows
 *   some bar, there's no way to render just a name with nothing else.
 */
public final class HerobrineEncounter implements Listener {
    public enum EncounterType { OBSERVE, FLEE_ON_SIGHT, BEHIND, FALSE_CHASE, OUTSIDE_OBSERVER, WATCHER, LURKING, STALKING, CREEPING }

    private static final NamespacedKey MARKER = new NamespacedKey("caveslite", "herobrine-marker");
    private static final Set<Material> PROTECTED_NEARBY = EnumSet.of(
            Material.VAULT, Material.TRIAL_SPAWNER,
            Material.SCULK_CATALYST, Material.REINFORCED_DEEPSLATE
    );

    private record Active(
            UUID entityId,
            EncounterType type,
            long spawnTick,
            java.util.concurrent.atomic.AtomicInteger torchesLeft,
            java.util.concurrent.atomic.AtomicLong nextTorchTick,
            java.util.concurrent.atomic.AtomicLong lookStartTick,
            java.util.concurrent.atomic.AtomicBoolean inCombat,
            java.util.concurrent.atomic.AtomicLong combatStartTick,
            java.util.concurrent.atomic.AtomicReference<BossBar> bossBar,
            UUID targetPlayerId,
            java.util.concurrent.atomic.AtomicBoolean spotted,
            java.util.concurrent.atomic.AtomicBoolean sneakyStrikeDone,
            java.util.concurrent.atomic.AtomicBoolean escaping,
            java.util.concurrent.atomic.AtomicLong escapeStartTick
    ) {}

    private final Plugin plugin;
    private final Lang lang;
    private final WorldFilter worlds = new WorldFilter();
    private final Map<UUID, Active> active = new HashMap<>();
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private final Map<UUID, Long> nextEncounter = new HashMap<>();

    // config
    private boolean enabled;
    private List<Biome> biomes = List.of();
    private int yMin, yMax;
    private long cooldownTicks;
    private long intervalMinTicks, intervalMaxTicks;
    private double minDistance, maxDistance, minDistanceBetween;
    private Set<EncounterType> enabledEncounters = EnumSet.allOf(EncounterType.class);
    private int maxLookSeconds;
    private double relocateChance, vanishChance;
    private Sound footstepSound;
    private float footstepVolume, footstepPitch;
    private boolean stopMessageEnabled;
    private double stopMessageChance;
    private boolean torchesEnabled;
    private double torchChance;
    private long torchCooldownTicks;
    private int torchMaxPerInstance;
    private boolean redstoneOnHitEnabled;
    private double redstoneOnHitChance;
    private String skinValue;
    private boolean combatEnabled;
    private boolean combatBossBarEnabled;
    private long combatMaxDurationTicks;
    private double fleeHealthFraction;
    private int fleeSpeedAmplifier;
    private double health, damage, speed, maxChaseDistance;
    private double combatKnockback, combatStep, fleeStep;
    private long combatAttackCooldownTicks, fleeDurationTicks;
    private double lurkingMinDistance, lurkingMaxDistance, lurkingLateral;
    private double stalkingMinDistance, stalkingMaxDistance, stalkingLateral;
    private double creepingMinDistance, creepingMaxDistance, creepingLateral;
    private long stalkingMaxLifetimeTicks;
    private long watcherMoveIntervalTicks;
    private double watcherStep;
    private boolean sneakyStrikeEnabled;
    private double sneakyStrikeChance, sneakyStrikeDamage, sneakyStrikeKnockback;
    private long sneakyStrikeDelayTicks;
    private int rewardXp;
    private boolean rewardCommandEnabled;
    private String rewardCommand;

    public HerobrineEncounter(Plugin plugin, Lang lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public void reload(ConfigurationSection cfg) {
        enabled = cfg.getBoolean("enabled", true);
        worlds.reload(cfg.getStringList("worlds"));
        yMin = cfg.getInt("y-min", -64);
        yMax = cfg.getInt("y-max", 320);
        cooldownTicks = Math.max(1, cfg.getLong("cooldown-seconds", 600)) * 20L;
        ConfigurationSection interval = cfg.getConfigurationSection("encounter-interval");
        intervalMinTicks = Math.max(30, interval != null ? interval.getLong("min-seconds", 480) : 480) * 20L;
        intervalMaxTicks = Math.max(intervalMinTicks, Math.max(60, interval != null ? interval.getLong("max-seconds", 1200) : 1200) * 20L);
        minDistance = cfg.getDouble("min-distance-from-player", 10);
        maxDistance = Math.max(minDistance + 1, cfg.getDouble("max-distance-from-player", 24));
        minDistanceBetween = cfg.getDouble("min-distance-between-instances", 32);

        biomes = new ArrayList<>();
        for (String name : cfg.getStringList("biomes")) {
            try {
                Biome biome = Registry.BIOME.get(NamespacedKey.minecraft(name.trim().toLowerCase(Locale.ROOT)));
                if (biome != null) {
                    biomes.add(biome);
                } else {
                    plugin.getLogger().log(Level.WARNING, "Unknown herobrine biome: {0}", name);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Unknown herobrine biome: {0}", name);
            }
        }

        enabledEncounters = EnumSet.noneOf(EncounterType.class);
        ConfigurationSection encountersCfg = cfg.getConfigurationSection("encounters");
        if (encountersCfg != null) {
            if (encountersCfg.getBoolean("observe", true)) enabledEncounters.add(EncounterType.OBSERVE);
            if (encountersCfg.getBoolean("flee-on-sight", true)) enabledEncounters.add(EncounterType.FLEE_ON_SIGHT);
            if (encountersCfg.getBoolean("behind", true)) enabledEncounters.add(EncounterType.BEHIND);
            if (encountersCfg.getBoolean("false-chase", true)) enabledEncounters.add(EncounterType.FALSE_CHASE);
            if (encountersCfg.getBoolean("outside-observer", true)) enabledEncounters.add(EncounterType.OUTSIDE_OBSERVER);
            if (encountersCfg.getBoolean("watcher", true)) enabledEncounters.add(EncounterType.WATCHER);
            if (encountersCfg.getBoolean("lurking", true)) enabledEncounters.add(EncounterType.LURKING);
            if (encountersCfg.getBoolean("stalking", true)) enabledEncounters.add(EncounterType.STALKING);
            if (encountersCfg.getBoolean("creeping", true)) enabledEncounters.add(EncounterType.CREEPING);
        }
        if (enabledEncounters.isEmpty()) enabledEncounters.add(EncounterType.OBSERVE);

        ConfigurationSection observation = cfg.getConfigurationSection("observation");
        maxLookSeconds = observation != null ? observation.getInt("max-look-seconds", 8) : 8;
        relocateChance = (observation != null ? observation.getDouble("teleport-chance", 20) : 20) / 100;
        vanishChance = (observation != null ? observation.getDouble("vanish-chance", 10) : 10) / 100;

        ConfigurationSection stalking = cfg.getConfigurationSection("stalking");
        lurkingMinDistance = stalking != null ? stalking.getDouble("lurking.min-distance", 50) : 50;
        lurkingMaxDistance = stalking != null ? stalking.getDouble("lurking.max-distance", 80) : 80;
        lurkingLateral = stalking != null ? stalking.getDouble("lurking.max-lateral", 50) : 50;
        stalkingMinDistance = stalking != null ? stalking.getDouble("stalking.min-distance", 25) : 25;
        stalkingMaxDistance = stalking != null ? stalking.getDouble("stalking.max-distance", 46) : 46;
        stalkingLateral = stalking != null ? stalking.getDouble("stalking.max-lateral", 35) : 35;
        creepingMinDistance = stalking != null ? stalking.getDouble("creeping.min-distance", 3) : 3;
        creepingMaxDistance = stalking != null ? stalking.getDouble("creeping.max-distance", 5) : 5;
        creepingLateral = stalking != null ? stalking.getDouble("creeping.max-lateral", 3) : 3;
        stalkingMaxLifetimeTicks = Math.max(5, stalking != null ? stalking.getLong("max-lifetime-seconds", 45) : 45) * 20L;
        ConfigurationSection watcherCfg = stalking != null ? stalking.getConfigurationSection("watcher") : null;
        watcherMoveIntervalTicks = Math.max(4, watcherCfg != null ? watcherCfg.getLong("move-interval-ticks", 10) : 10);
        watcherStep = Math.max(0.1, watcherCfg != null ? watcherCfg.getDouble("step-blocks", 0.8) : 0.8);
        ConfigurationSection strike = stalking != null ? stalking.getConfigurationSection("sneaky-strike") : null;
        sneakyStrikeEnabled = strike == null || strike.getBoolean("enabled", true);
        sneakyStrikeChance = (strike != null ? strike.getDouble("chance", 10) : 10) / 100;
        sneakyStrikeDelayTicks = Math.max(1, strike != null ? strike.getLong("delay-seconds", 8) : 8) * 20L;
        sneakyStrikeDamage = Math.max(0, strike != null ? strike.getDouble("damage", 2) : 2);
        sneakyStrikeKnockback = Math.max(0, strike != null ? strike.getDouble("knockback", 0.7) : 0.7);

        ConfigurationSection footsteps = cfg.getConfigurationSection("footsteps");
        footstepSound = Sounds.find(footsteps != null ? footsteps.getString("sound", "minecraft:entity.player.attack.weak") : "minecraft:entity.player.attack.weak");
        footstepVolume = (float) (footsteps != null ? footsteps.getDouble("volume", 0.6) : 0.6);
        footstepPitch = (float) (footsteps != null ? footsteps.getDouble("pitch", 0.6) : 0.6);

        ConfigurationSection stop = cfg.getConfigurationSection("stop-message");
        stopMessageEnabled = stop == null || stop.getBoolean("enabled", true);
        stopMessageChance = (stop != null ? stop.getDouble("chance", 0.5) : 0.5) / 100;

        ConfigurationSection torches = cfg.getConfigurationSection("redstone-torches");
        torchesEnabled = torches == null || torches.getBoolean("enabled", true);
        torchChance = (torches != null ? torches.getDouble("chance", 15) : 15) / 100;
        torchCooldownTicks = Math.max(1, torches != null ? torches.getLong("cooldown-seconds", 600) : 600) * 20L;
        torchMaxPerInstance = torches != null ? torches.getInt("max-per-instance", 5) : 5;

        ConfigurationSection onHit = cfg.getConfigurationSection("redstone-on-hit");
        redstoneOnHitEnabled = onHit == null || onHit.getBoolean("enabled", true);
        redstoneOnHitChance = (onHit != null ? onHit.getDouble("chance", 10) : 10) / 100;

        skinValue = cfg.getString("skin-value",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOThiN2NhM2M3ZDMxNGE2MWFiZWQ4ZmMxOGQ3OTdmYzMwYjZlZmM4NDQ1NDI1YzRlMjUwOTk3ZTUyZTZjYiJ9fX0=");

        ConfigurationSection combat = cfg.getConfigurationSection("combat");
        combatEnabled = combat == null || combat.getBoolean("enabled", true);
        combatBossBarEnabled = combat != null && combat.getBoolean("bossbar-enabled", false);
        combatMaxDurationTicks = Math.max(1, combat != null ? combat.getLong("max-duration-seconds", 60) : 60) * 20L;
        fleeHealthFraction = combat != null ? combat.getDouble("flee-health-fraction", 0.35) : 0.35;
        fleeSpeedAmplifier = combat != null ? combat.getInt("flee-speed-amplifier", 2) : 2;

        health = Math.max(1, cfg.getDouble("health", 40));
        damage = Math.max(0, cfg.getDouble("damage", 2));
        speed = cfg.getDouble("speed", 0.35);
        combatKnockback = combat != null ? Math.max(0, combat.getDouble("knockback", 1.15)) : 1.15;
        combatStep = combat != null ? Math.max(0.1, combat.getDouble("chase-step-blocks", 0.55)) : 0.55;
        fleeStep = combat != null ? Math.max(0.2, combat.getDouble("flee-step-blocks", 1.05)) : 1.05;
        combatAttackCooldownTicks = Math.max(8, combat != null ? combat.getLong("attack-cooldown-ticks", 24) : 24);
        fleeDurationTicks = Math.max(20, combat != null ? combat.getLong("flee-duration-seconds", 4) : 4) * 20L;
        maxChaseDistance = cfg.getDouble("max-chase-distance", 20);

        ConfigurationSection reward = cfg.getConfigurationSection("reward");
        rewardXp = reward != null ? reward.getInt("xp", 30) : 30;
        ConfigurationSection rewardCmd = reward != null ? reward.getConfigurationSection("command") : null;
        rewardCommandEnabled = rewardCmd != null && rewardCmd.getBoolean("enabled", false);
        rewardCommand = rewardCmd != null ? rewardCmd.getString("command", "") : "";
    }

    // -------------------------------------------------------------- spawning

    /** Called periodically (see check-interval in CavesLite) - tries a spawn roll for each online player. */
    public void trySpawns() {
        if (!enabled) return;
        long now = firstWorldTime();

        for (World world : Bukkit.getWorlds()) {
            if (!worlds.allows(world)) continue;
            for (Player player : world.getPlayers()) {
                UUID pid = player.getUniqueId();
                Long cooldown = playerCooldowns.get(pid);
                if (cooldown != null && now < cooldown) continue;
                long due = nextEncounter.computeIfAbsent(pid, ignored -> now + randomEncounterDelay());
                if (now < due) continue;
                if (player.getLocation().getBlockY() < yMin || player.getLocation().getBlockY() > yMax) { nextEncounter.put(pid, now + 1200L); continue; }
                if (!biomes.isEmpty() && !biomes.contains(player.getLocation().getBlock().getBiome())) { nextEncounter.put(pid, now + 1200L); continue; }

                if (trySpawnNear(player, now)) nextEncounter.put(pid, now + randomEncounterDelay());
                else nextEncounter.put(pid, now + 1200L); // retry in one minute when placement/conditions failed
            }
        }
    }

    private boolean trySpawnNear(Player player, long now) {
        EncounterType type = pickEncounterType(player);
        if (type == null) return false;

        Location spot = findEncounterSpot(player, type);
        if (spot == null && Locations.isCave(player.getLocation())) {
            // In caves, never waste a due encounter merely because a long-range type had no corridor.
            for (EncounterType fallback : List.of(EncounterType.STALKING, EncounterType.WATCHER, EncounterType.CREEPING)) {
                if (!enabledEncounters.contains(fallback) || fallback == type) continue;
                spot = findEncounterSpot(player, fallback);
                if (spot != null) { type = fallback; break; }
            }
        }
        if (spot == null) return false;

        if (isNearProtectedStructure(spot) || isTooCloseToAnotherInstance(spot)) return false;

        LivingEntity entity = spawn(spot);
        UUID id = entity.getUniqueId();
        Active instance = new Active(
                id, type, now,
                new java.util.concurrent.atomic.AtomicInteger(0),
                new java.util.concurrent.atomic.AtomicLong(0),
                new java.util.concurrent.atomic.AtomicLong(0),
                new java.util.concurrent.atomic.AtomicBoolean(false),
                new java.util.concurrent.atomic.AtomicLong(0),
                new java.util.concurrent.atomic.AtomicReference<>(null),
                player.getUniqueId(),
                new java.util.concurrent.atomic.AtomicBoolean(false),
                new java.util.concurrent.atomic.AtomicBoolean(false),
                new java.util.concurrent.atomic.AtomicBoolean(false),
                new java.util.concurrent.atomic.AtomicLong(0)
        );
        active.put(id, instance);
        playerCooldowns.put(player.getUniqueId(), now + cooldownTicks);

        if (type == EncounterType.BEHIND) {
            positionBehind(entity, player);
            Locations.playSound(entity.getLocation(), footstepSound, SoundCategory.HOSTILE, footstepVolume, footstepPitch);
        }
        return true;
    }

    private long randomEncounterDelay() {
        if (intervalMaxTicks <= intervalMinTicks) return intervalMinTicks;
        return (long) Rng.nextDouble(intervalMinTicks, intervalMaxTicks + 1);
    }

    private EncounterType pickEncounterType(Player player) {
        List<EncounterType> options = new ArrayList<>(enabledEncounters);
        boolean inCave = Locations.isCave(player.getLocation());
        if (options.contains(EncounterType.OUTSIDE_OBSERVER) && inCave) options.remove(EncounterType.OUTSIDE_OBSERVER);
        if (options.isEmpty()) return null;
        return Rng.randomElement(options);
    }

    private boolean isStalkingType(EncounterType type) {
        return type == EncounterType.LURKING || type == EncounterType.STALKING || type == EncounterType.CREEPING;
    }

    private Location findEncounterSpot(Player player, EncounterType type) {
        if (Locations.isCave(player.getLocation())) return findCaveSpot(player, type);
        return isStalkingType(type) ? findStalkingSpot(player, type) : findSpot(player, type);
    }

    /** Cave-aware placement: searches the player's actual underground height instead of the surface. */
    private Location findCaveSpot(Player player, EncounterType type) {
        double min = (type == EncounterType.CREEPING ? 5 : type == EncounterType.WATCHER ? 7 : 10);
        double max = (type == EncounterType.LURKING ? 30 : type == EncounterType.STALKING ? 24 : type == EncounterType.CREEPING ? 12 : 18);
        Location base = player.getLocation();
        World world = base.getWorld();
        if (world == null) return null;
        for (int attempt = 0; attempt < 48; attempt++) {
            double distance = Rng.nextDouble(min, max);
            double angle = Rng.nextDouble(0, Math.PI * 2);
            Location column = base.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            if (!world.isChunkLoaded(column.getBlockX() >> 4, column.getBlockZ() >> 4)) continue;
            for (int dy = 6; dy >= -6; dy--) {
                Location candidate = new Location(world, column.getX(), base.getBlockY() + dy, column.getZ());
                Block feet = candidate.getBlock();
                if (!isSafeSpawnColumn(feet, feet.getRelative(0, 1, 0), feet.getRelative(0, -1, 0))) continue;
                candidate.add(0.5, 0, 0.5);
                if (candidate.distanceSquared(base) < min * min) continue;
                facePlayer(candidate, player);
                return candidate;
            }
        }
        return null;
    }

    /**
     * From-The-Fog-inspired placement: choose a point relative to the direction the player is facing,
     * add a random lateral offset, then find nearby solid ground without force-loading chunks.
     */
    private Location findStalkingSpot(Player player, EncounterType type) {
        double min, max, lateral;
        boolean allowFront = type != EncounterType.CREEPING;
        switch (type) {
            case LURKING -> { min = lurkingMinDistance; max = lurkingMaxDistance; lateral = lurkingLateral; }
            case STALKING -> { min = stalkingMinDistance; max = stalkingMaxDistance; lateral = stalkingLateral; }
            case CREEPING -> { min = creepingMinDistance; max = creepingMaxDistance; lateral = creepingLateral; }
            default -> { return findSpot(player, type); }
        }

        org.bukkit.util.Vector forward = player.getEyeLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 0.001) forward = new org.bukkit.util.Vector(0, 0, 1);
        forward.normalize();
        org.bukkit.util.Vector right = new org.bukkit.util.Vector(-forward.getZ(), 0, forward.getX());

        for (int attempt = 0; attempt < 12; attempt++) {
            double distance = Rng.nextDouble(min, Math.max(min + 0.01, max));
            double side = Rng.nextDouble(-lateral, lateral);
            double sign = (allowFront && Rng.chance(0.5)) ? 1.0 : -1.0;
            org.bukkit.util.Vector offset = forward.clone().multiply(distance * sign).add(right.clone().multiply(side));
            Location column = player.getLocation().clone().add(offset);
            World world = column.getWorld();
            if (world == null || !world.isChunkLoaded(column.getBlockX() >> 4, column.getBlockZ() >> 4)) continue;

            int centerY = player.getLocation().getBlockY();
            for (int dy = 8; dy >= -8; dy--) {
                int y = centerY + dy;
                if (y < yMin || y > yMax) continue;
                Location candidate = new Location(world, column.getX(), y, column.getZ());
                Block feet = candidate.getBlock();
                Block headBlock = feet.getRelative(0, 1, 0);
                Block floor = feet.getRelative(0, -1, 0);
                if (isSafeSpawnColumn(feet, headBlock, floor)) {
                    candidate.add(0.5, 0, 0.5);
                    facePlayer(candidate, player);
                    return candidate;
                }
            }
        }
        return null;
    }

    private void facePlayer(Location from, Player player) {
        org.bukkit.util.Vector direction = player.getEyeLocation().toVector().subtract(from.toVector());
        if (direction.lengthSquared() > 0.001) from.setDirection(direction);
    }

    private Location findSpot(Player player, EncounterType type) {
        Location base = player.getLocation();
        for (int attempt = 0; attempt < 6; attempt++) {
            double distance = Rng.nextDouble(minDistance, maxDistance);
            double angle = Rng.nextDouble(0, Math.PI * 2);
            double dx = Math.cos(angle) * distance;
            double dz = Math.sin(angle) * distance;
            Location candidate = base.clone().add(dx, 0, dz);
            World world = candidate.getWorld();
            if (world == null) continue;

            int highestY = world.getHighestBlockYAt(candidate);
            candidate.setY(Math.min(Math.max(highestY + 1, yMin), yMax));

            Block feet = candidate.getBlock();
            Block headBlock = feet.getRelative(0, 1, 0);
            Block floor = feet.getRelative(0, -1, 0);
            if (!isSafeSpawnColumn(feet, headBlock, floor)) continue;
            candidate.add(0.5, 0, 0.5);
            facePlayer(candidate, player);
            return candidate;
        }
        return null;
    }


    private boolean isSafeSpawnColumn(Block feet, Block headBlock, Block floor) {
        if (!feet.isPassable() || !headBlock.isPassable()) return false;
        Material ground = floor.getType();
        if (!ground.isSolid()) return false;
        if (floor.isLiquid() || feet.isLiquid() || headBlock.isLiquid()) return false;
        return ground != Material.MAGMA_BLOCK && ground != Material.CACTUS && ground != Material.CAMPFIRE
                && ground != Material.SOUL_CAMPFIRE && ground != Material.POWDER_SNOW
                && ground != Material.FIRE && ground != Material.SOUL_FIRE;
    }

    private boolean isNearProtectedStructure(Location spot) {
        for (int x = -4; x <= 4; x += 2) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -4; z <= 4; z += 2) {
                    if (PROTECTED_NEARBY.contains(spot.clone().add(x, y, z).getBlock().getType())) return true;
                }
            }
        }
        return false;
    }

    private boolean isTooCloseToAnotherInstance(Location spot) {
        double minSquared = minDistanceBetween * minDistanceBetween;
        for (Active instance : active.values()) {
            Entity other = Bukkit.getEntity(instance.entityId());
            if (other != null && other.getLocation().distanceSquared(spot) < minSquared) return true;
        }
        return false;
    }

    private LivingEntity spawn(Location loc) {
        Mannequin entity = (Mannequin) loc.getWorld().spawnEntity(loc, EntityType.MANNEQUIN);
        entity.getPersistentDataContainer().set(MARKER, PersistentDataType.BYTE, (byte) 1);
        entity.setSilent(true);
        entity.setCustomNameVisible(false);
        entity.setDescription(null);
        entity.setImmovable(true);
        entity.setInvulnerable(false);
        entity.setPersistent(false);

        dev.caveslite.util.Utils.setMaxHealth(entity, health);

        // Mannequin renders a real player-shaped body. Reuse the configured texture property
        // so Herobrine has a complete skin instead of a Zombie body with a player head.
        if (skinValue != null && !skinValue.isBlank()) {
            try {
                ResolvableProfile profile = ResolvableProfile.resolvableProfile()
                        .name("Herobrine")
                        .uuid(UUID.nameUUIDFromBytes(("CavesLite:Herobrine:" + skinValue.hashCode()).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .addProperty(new ProfileProperty("textures", skinValue))
                        .build();
                entity.setProfile(profile);
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.WARNING, "Could not apply Herobrine mannequin skin; using default mannequin profile", ex);
            }
        }
        return entity;
    }

    private void positionBehind(LivingEntity entity, Player player) {
        Location loc = player.getLocation().subtract(player.getLocation().getDirection().setY(0).normalize().multiply(2.5));
        loc.setDirection(player.getLocation().getDirection().multiply(-1));
        entity.teleport(loc);
    }

    // ---------------------------------------------------------------- ticking

    /** Called frequently (every few ticks) for the observation/vanish/torch/stop-message behaviour. */
    public void tick() {
        if (active.isEmpty()) return;
        long now = firstWorldTime();

        for (UUID id : new ArrayList<>(active.keySet())) {
            Active instance = active.get(id);
            Entity raw = Bukkit.getEntity(id);
            if (!(raw instanceof LivingEntity entity) || !entity.isValid()) {
                cleanup(id);
                continue;
            }

            if (instance.inCombat().get()) {
                tickCombat(entity, instance, now);
            } else {
                tickIdle(entity, instance, now);
            }
        }
    }

    private long firstWorldTime() {
        List<World> worldsList = Bukkit.getWorlds();
        return worldsList.isEmpty() ? 0 : worldsList.get(0).getFullTime();
    }

    private void tickIdle(LivingEntity entity, Active instance, long now) {
        Player target = Bukkit.getPlayer(instance.targetPlayerId());
        if (target != null && target.isOnline() && target.getWorld().equals(entity.getWorld())) {
            // A stalker never idles facing a random direction: keep the classic white eyes on its victim.
            Location facing = entity.getLocation();
            facePlayer(facing, target);
            entity.teleport(facing);
        }

        Player watcher = nearestWatcher(entity);
        if (watcher != null) {
            entity.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            instance.spotted().set(true);
            long lookStart = instance.lookStartTick().get();
            if (lookStart == 0) {
                instance.lookStartTick().set(now);
            } else if ((now - lookStart) > maxLookSeconds * 20L) {
                vanish(entity, instance);
                return;
            }
        } else {
            instance.lookStartTick().set(0);
            if (!isStalkingType(instance.type())) {
                if (Rng.chance(vanishChance)) {
                    vanish(entity, instance);
                    return;
                }
                if (Rng.chance(relocateChance)) relocate(entity);
            }
        }

        if (instance.type() == EncounterType.WATCHER && target != null && target.isOnline()) {
            // Creaking-inspired behaviour: freeze while watched; approach only when the player looks away.
            if (watcher == null && (now - instance.spawnTick()) % watcherMoveIntervalTicks < 4) {
                stepTowardTarget(entity, target);
            }
        }

        if (isStalkingType(instance.type()) || instance.type() == EncounterType.WATCHER) {
            if (now - instance.spawnTick() >= stalkingMaxLifetimeTicks) {
                vanish(entity, instance);
                return;
            }
            // Creeping can punish a player who never notices the figure behind them. One strike, then vanish.
            if (instance.type() == EncounterType.CREEPING && sneakyStrikeEnabled && !instance.spotted().get()
                    && !instance.sneakyStrikeDone().get() && now - instance.spawnTick() >= sneakyStrikeDelayTicks) {
                instance.sneakyStrikeDone().set(true);
                if (target != null && target.isOnline() && target.getWorld().equals(entity.getWorld())
                        && target.getLocation().distanceSquared(entity.getLocation()) <= 7 * 7 && Rng.chance(sneakyStrikeChance)) {
                    if (sneakyStrikeDamage > 0) target.damage(sneakyStrikeDamage, entity);
                    org.bukkit.util.Vector push = target.getLocation().toVector().subtract(entity.getLocation().toVector());
                    if (push.lengthSquared() > 0.001) target.setVelocity(push.normalize().multiply(sneakyStrikeKnockback).setY(0.25));
                    vanish(entity, instance);
                    return;
                }
            }
        }

        maybeDropTorch(entity, instance, now);
        maybeSendStopMessage(entity);
    }

    private void stepTowardTarget(LivingEntity entity, Player target) {
        org.bukkit.util.Vector delta = target.getLocation().toVector().subtract(entity.getLocation().toVector()).setY(0);
        if (delta.lengthSquared() < 4.0) return;
        Location next = entity.getLocation().clone().add(delta.normalize().multiply(watcherStep));
        Block feet = next.getBlock();
        Block headBlock = feet.getRelative(0, 1, 0);
        Block floor = feet.getRelative(0, -1, 0);
        if (!isSafeSpawnColumn(feet, headBlock, floor)) return;
        facePlayer(next, target);
        entity.teleport(next);
    }

    private void tickCombat(LivingEntity entity, Active instance, long now) {
        Player target = Bukkit.getPlayer(instance.targetPlayerId());
        if (target == null || !target.isOnline() || !target.getWorld().equals(entity.getWorld())) {
            endCombat(instance); vanish(entity, instance); return;
        }

        long elapsed = now - instance.combatStartTick().get();
        double fraction = entity.getHealth() / Math.max(1, health);
        if (!instance.escaping().get() && (elapsed >= combatMaxDurationTicks || fraction <= fleeHealthFraction)) {
            instance.escaping().set(true);
            instance.escapeStartTick().set(now);
        }

        if (instance.escaping().get()) {
            // Herobrine does not blink out at timeout: he visibly bolts away first.
            if ((now - instance.escapeStartTick().get()) >= fleeDurationTicks ||
                    target.getLocation().distanceSquared(entity.getLocation()) > maxChaseDistance * maxChaseDistance * 2.25) {
                endCombat(instance); vanish(entity, instance); return;
            }
            stepAwayFromTarget(entity, target, fleeStep);
            return;
        }

        double distanceSq = target.getLocation().distanceSquared(entity.getLocation());
        if (distanceSq > maxChaseDistance * maxChaseDistance) {
            instance.escaping().set(true); instance.escapeStartTick().set(now); return;
        }

        // Frequent short steps make the mannequin feel alive without mob pathfinding.
        if (elapsed % 4L == 0L) stepTowardTarget(entity, target, combatStep);
        if (distanceSq <= 2.5 * 2.5 && elapsed % combatAttackCooldownTicks < 4L) {
            target.damage(damage, entity);
            org.bukkit.util.Vector push = target.getLocation().toVector().subtract(entity.getLocation().toVector()).setY(0);
            if (push.lengthSquared() > 0.001) target.setVelocity(push.normalize().multiply(combatKnockback).setY(0.28));
        }
    }

    private void stepTowardTarget(LivingEntity entity, Player target, double step) {
        org.bukkit.util.Vector delta = target.getLocation().toVector().subtract(entity.getLocation().toVector()).setY(0);
        if (delta.lengthSquared() < 1.0) return;
        Location next = entity.getLocation().clone().add(delta.normalize().multiply(step));
        if (!canOccupy(next)) return;
        facePlayer(next, target);
        entity.teleport(next);
    }

    private void stepAwayFromTarget(LivingEntity entity, Player target, double step) {
        org.bukkit.util.Vector delta = entity.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0);
        if (delta.lengthSquared() < 0.01) return;
        Location next = entity.getLocation().clone().add(delta.normalize().multiply(step));
        if (!canOccupy(next)) return;
        facePlayer(next, target);
        entity.teleport(next);
    }

    private boolean canOccupy(Location location) {
        Block feet = location.getBlock();
        return isSafeSpawnColumn(feet, feet.getRelative(0, 1, 0), feet.getRelative(0, -1, 0));
    }

    private Player nearestWatcher(LivingEntity entity) {
        // LURKING can intentionally be much farther away than the normal observer range.
        // Iterating world players avoids a large nearby-entity cube scan and still keeps this cheap.
        double maxDetection = Math.max(100.0, lurkingMaxDistance + lurkingLateral);
        double maxDetectionSq = maxDetection * maxDetection;
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player player : entity.getWorld().getPlayers()) {
            double distanceSq = player.getLocation().distanceSquared(entity.getLocation());
            if (distanceSq > maxDetectionSq) continue;
            if (!Locations.isLookingAt(player, entity) || !player.hasLineOfSight(entity)) continue;
            if (distanceSq < best) {
                best = distanceSq;
                nearest = player;
            }
        }
        return nearest;
    }

    private void relocate(LivingEntity entity) {
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity e : entity.getWorld().getNearbyEntities(entity.getLocation(), 40, 40, 40)) {
            if (e instanceof Player player) {
                double d = player.getLocation().distanceSquared(entity.getLocation());
                if (d < best) { best = d; nearest = player; }
            }
        }
        if (nearest == null) return;
        Location spot = findSpot(nearest, EncounterType.OBSERVE);
        if (spot != null) entity.teleport(spot);
    }

    private void vanish(LivingEntity entity, Active instance) {
        Locations.playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.6f, 0.6f);
        entity.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, entity.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.01);
        cleanup(instance.entityId());
        entity.remove();
    }

    private void cleanup(UUID id) {
        Active instance = active.remove(id);
        if (instance != null) {
            BossBar bar = instance.bossBar().get();
            if (bar != null) {
                for (World world : Bukkit.getWorlds()) {
                    for (Player player : world.getPlayers()) player.hideBossBar(bar);
                }
            }
        }
    }

    private void maybeDropTorch(LivingEntity entity, Active instance, long now) {
        if (!torchesEnabled) return;
        if (instance.torchesLeft().get() >= torchMaxPerInstance) return;
        if (now < instance.nextTorchTick().get()) return;
        if (!Rng.chance(torchChance)) return;

        Block placed = findTorchSpot(entity.getLocation());
        if (placed != null) {
            placed.setType(Material.REDSTONE_TORCH, false);
            instance.torchesLeft().incrementAndGet();
            instance.nextTorchTick().set(now + torchCooldownTicks);
        }
    }

    private Block findTorchSpot(Location origin) {
        // Only places a torch into existing air above a safe solid block; no terrain is removed/replaced.
        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = (int) Math.floor(Rng.nextDouble(-4, 5));
            int dz = (int) Math.floor(Rng.nextDouble(-4, 5));
            int dy = (int) Math.floor(Rng.nextDouble(-2, 3));
            Block air = origin.getBlock().getRelative(dx, dy, dz);
            Block floor = air.getRelative(0, -1, 0);
            if (!air.getType().isAir() || !floor.getType().isSolid() || floor.isLiquid()) continue;
            Material ground = floor.getType();
            if (ground == Material.MAGMA_BLOCK || ground == Material.CACTUS || ground == Material.POWDER_SNOW) continue;
            return air;
        }
        return null;
    }

    private void maybeSendStopMessage(LivingEntity entity) {
        if (!stopMessageEnabled || !Rng.chance(stopMessageChance)) return;
        Player watcher = nearestWatcher(entity);
        if (watcher != null) {
            Text.send(watcher, lang.get("herobrine.stop-message"));
        }
    }

    // ---------------------------------------------------------------- combat

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim) || !isHerobrine(victim)) return;
        Active instance = active.get(victim.getUniqueId());
        if (instance == null) return;

        if (redstoneOnHitEnabled && Rng.chance(redstoneOnHitChance)) {
            victim.getWorld().dropItemNaturally(victim.getLocation(), new ItemStack(Material.REDSTONE));
        }

        if (!combatEnabled) {
            event.setCancelled(true);
            return;
        }
        if (!instance.inCombat().get() && event.getDamager() instanceof Player attacker) {
            startCombat(victim, instance, attacker);
        }
    }

    private void startCombat(LivingEntity entity, Active instance, Player attacker) {
        instance.inCombat().set(true);
        instance.combatStartTick().set(firstWorldTime());
        instance.escaping().set(false);
        instance.escapeStartTick().set(0);
        entity.setInvulnerable(false);
        entity.setCustomNameVisible(false);
        if (entity instanceof Mannequin mannequin) mannequin.setImmovable(false);

        if (combatBossBarEnabled) {
            BossBar bar = BossBar.bossBar(Text.legacy(lang.get("herobrine.bossbar-name")), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
            instance.bossBar().set(bar);
            for (Player player : entity.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(entity.getLocation()) <= 40 * 40) {
                    player.showBossBar(bar);
                }
            }
        }
    }

    private void flee(LivingEntity entity) {
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player player : entity.getWorld().getPlayers()) {
            double d = player.getLocation().distanceSquared(entity.getLocation());
            if (d < best) { best = d; nearest = player; }
        }
        if (nearest != null) stepAwayFromTarget(entity, nearest, 0.65);
    }

    private void endCombat(Active instance) {
        instance.inCombat().set(false);
        BossBar bar = instance.bossBar().getAndSet(null);
        if (bar != null) {
            for (World world : Bukkit.getWorlds()) {
                for (Player player : world.getPlayers()) player.hideBossBar(bar);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!isHerobrine(dead)) return;
        Active instance = active.get(dead.getUniqueId());

        event.getDrops().clear();
        event.setDroppedExp(rewardXp);

        if (rewardCommandEnabled && rewardCommand != null && !rewardCommand.isBlank()
                && dead.getKiller() instanceof Player killer) {
            String parsed = rewardCommand.replace("{player}", killer.getName());
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(console, parsed));
        }

        if (instance != null) {
            endCombat(instance);
            active.remove(dead.getUniqueId());
        }
    }

    private boolean isHerobrine(LivingEntity entity) {
        return entity.getPersistentDataContainer().has(MARKER, PersistentDataType.BYTE);
    }

    public boolean summonFor(Player target, String requestedType) {
        EncounterType type;
        try { type = requestedType == null ? EncounterType.STALKING : EncounterType.valueOf(requestedType.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return false; }
        long now = firstWorldTime();
        Location spot = findEncounterSpot(target, type);
        if (spot == null || isNearProtectedStructure(spot)) return false;
        LivingEntity entity = spawn(spot);
        Active instance = new Active(entity.getUniqueId(), type, now,
                new java.util.concurrent.atomic.AtomicInteger(0), new java.util.concurrent.atomic.AtomicLong(0),
                new java.util.concurrent.atomic.AtomicLong(0), new java.util.concurrent.atomic.AtomicBoolean(false),
                new java.util.concurrent.atomic.AtomicLong(0), new java.util.concurrent.atomic.AtomicReference<>(null),
                target.getUniqueId(), new java.util.concurrent.atomic.AtomicBoolean(false), new java.util.concurrent.atomic.AtomicBoolean(false),
                new java.util.concurrent.atomic.AtomicBoolean(false), new java.util.concurrent.atomic.AtomicLong(0));
        active.put(entity.getUniqueId(), instance);
        return true;
    }

    public String debug(Player player) {
        long now = firstWorldTime();
        long due = nextEncounter.computeIfAbsent(player.getUniqueId(), ignored -> now + randomEncounterDelay());
        long seconds = Math.max(0, (due - now) / 20L);
        long own = active.values().stream().filter(a -> a.targetPlayerId().equals(player.getUniqueId())).count();
        return "Herobrine: active=" + own + ", next=" + seconds + "s, enabled=" + enabled + ", encounters=" + enabledEncounters;
    }

    /** Removes every active Herobrine (used on plugin disable/reload cleanup). */
    public void removeAll() {
        for (UUID id : new ArrayList<>(active.keySet())) {
            Entity entity = Bukkit.getEntity(id);
            cleanup(id);
            if (entity != null) entity.remove();
        }
    }
}
