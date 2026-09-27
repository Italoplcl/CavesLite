package dev.caveslite;

import dev.caveslite.mobs.MobManager;
import dev.caveslite.mobs.defaults.AlphaSpider;
import dev.caveslite.mobs.defaults.CaveGolem;
import dev.caveslite.mobs.defaults.CryingBat;
import dev.caveslite.mobs.defaults.DeadMiner;
import dev.caveslite.mobs.defaults.HexedArmor;
import dev.caveslite.mobs.defaults.HungeringDarkness;
import dev.caveslite.mobs.defaults.LavaCreeper;
import dev.caveslite.mobs.defaults.MagmaMonster;
import dev.caveslite.mobs.defaults.Mimic;
import dev.caveslite.mobs.defaults.SmokeDemon;
import dev.caveslite.mobs.defaults.TNTCreeper;
import dev.caveslite.mobs.defaults.Watcher;
import dev.caveslite.util.Utils;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Lite port of Dangerous Caves 2 (MIT, by imDaniX and Evil-Lootlye):
 * only the ambient cave sounds and the custom mobs.
 */
public final class CavesLite extends JavaPlugin implements Listener {
    /** Ticks between "entity" updates of ticking mobs (same as the original). */
    private static final long MOB_TICK = 4L;
    /** Ticks between ambient sound checks (same as the original PLAYER tick). */
    private static final long AMBIENT_TICK = 800L;
    /** Ticks between ghost-footstep checks. */
    private static final long FOOTSTEPS_TICK = 60L;
    /** Ticks between tension-heartbeat checks (the interval settings in config.yml are on top of this). */
    private static final long TENSION_TICK = 10L;
    /** Ticks between boss-bar refreshes. */
    private static final long BOSS_BAR_TICK = 10L;
    /** Ticks between action-bar warning checks. */
    private static final long WARNINGS_TICK = 20L;
    /** Ticks between mob-trail particle checks. */
    private static final long TRAILS_TICK = 6L;
    /** Ticks between leaderboard re-sort + save checks (only does work if something changed). */
    private static final long LEADERBOARD_TICK = 100L;

    private MobManager mobs;
    private AmbientSounds ambient;
    private Footsteps footsteps;
    private Tension tension;
    private MobBossBar bossBar;
    private MobSpawnTitles spawnTitles;
    private MobWarnings warnings;
    private MobTrails trails;
    private VaultEconomy economy;
    private MobAchievements achievements;
    private KillLeaderboard leaderboard;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        mobs = new MobManager(this);
        mobs.register(new Mimic(mobs));
        mobs.register(new CaveGolem(mobs));
        mobs.register(new AlphaSpider());
        mobs.register(new HexedArmor());
        mobs.register(new MagmaMonster());
        mobs.register(new HungeringDarkness());
        mobs.register(new CryingBat());
        mobs.register(new Watcher());
        mobs.register(new TNTCreeper());
        mobs.register(new LavaCreeper());
        mobs.register(new DeadMiner());
        mobs.register(new SmokeDemon());

        ambient = new AmbientSounds(this);
        footsteps = new Footsteps(this);
        tension = new Tension(this, mobs);
        bossBar = new MobBossBar();
        spawnTitles = new MobSpawnTitles();
        warnings = new MobWarnings();
        trails = new MobTrails(getLogger());
        economy = new VaultEconomy(this);
        achievements = new MobAchievements(this, economy);
        leaderboard = new KillLeaderboard(this);

        mobs.addSpawnListener(spawnTitles);

        economy.hook();
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CavesPlaceholders(this, achievements, leaderboard).register();
            getLogger().info("PlaceholderAPI found - registered %dangerouscaves_*% placeholders.");
        }

        getServer().getPluginManager().registerEvents(mobs, this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(achievements, this);
        getServer().getPluginManager().registerEvents(leaderboard, this);
        reloadAll();

        getServer().getScheduler().runTaskTimer(this, mobs::tick, MOB_TICK, MOB_TICK);
        getServer().getScheduler().runTaskTimer(this, ambient::tick, AMBIENT_TICK, AMBIENT_TICK);
        getServer().getScheduler().runTaskTimer(this, footsteps::tick, FOOTSTEPS_TICK, FOOTSTEPS_TICK);
        getServer().getScheduler().runTaskTimer(this, tension::tick, TENSION_TICK, TENSION_TICK);
        getServer().getScheduler().runTaskTimer(this, bossBar::tick, BOSS_BAR_TICK, BOSS_BAR_TICK);
        getServer().getScheduler().runTaskTimer(this, warnings::tick, WARNINGS_TICK, WARNINGS_TICK);
        getServer().getScheduler().runTaskTimer(this, trails::tick, TRAILS_TICK, TRAILS_TICK);
        getServer().getScheduler().runTaskTimer(this, leaderboard::tick, LEADERBOARD_TICK, LEADERBOARD_TICK);

        PluginCommand command = getCommand("dangerouscaves");
        if (command != null) {
            command.setExecutor(new CavesCommand(this, mobs));
        }
    }

    /** Re-reads config.yml and applies it. */
    public void reloadAll() {
        reloadConfig();
        // The old plugin kept ambient sounds under "caverns.ambient"; this one accepts both layouts.
        var config = getConfig();
        var ambientSection = config.isConfigurationSection("caverns.ambient")
                ? Utils.section(config, "caverns.ambient")
                : Utils.section(config, "ambient");
        ambient.reload(ambientSection);
        footsteps.reload(Utils.section(ambientSection, "footsteps"));
        tension.reload(Utils.section(config, "tension"));
        bossBar.reload(Utils.section(config, "boss-bar"));
        spawnTitles.reload(Utils.section(config, "spawn-titles"));
        warnings.reload(Utils.section(config, "warnings"));
        trails.reload(Utils.section(config, "trails"));
        economy.hook();
        achievements.reload(Utils.section(config, "achievements"));
        leaderboard.reload(Utils.section(config, "leaderboards"));
        mobs.reload(Utils.section(config, "mobs"));
    }

    @Override
    public void onDisable() {
        if (leaderboard != null) {
            leaderboard.flush();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        bossBar.onQuit(event.getPlayer());
    }

    public MobAchievements getAchievements() {
        return achievements;
    }
}
