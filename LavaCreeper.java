package dev.caveslite.mobs.defaults;

import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.Locations;
import dev.caveslite.util.Rng;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** When it blows up it leaves a crater of fire, magma, obsidian and lava. */
public class LavaCreeper extends MobBase implements CustomMob.Ticking, Listener {
    private static final PotionEffect FIRE_RESISTANCE =
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false);

    private double changeChance;
    private double fire;
    private double magmaBlock;
    private double obsidian;
    private double lava;
    private int fireTouch;
    private int radius;

    public LavaCreeper() {
        super(EntityType.CREEPER, "lava-creeper", 6, null);
    }

    @Override
    protected void configure(ConfigurationSection cfg) {
        changeChance = cfg.getDouble("change-chance", 50) / 100;
        fire = cfg.getDouble("block-chances.fire", 33.33) / 100;
        magmaBlock = cfg.getDouble("block-chances.magma_block", 25) / 100;
        obsidian = cfg.getDouble("block-chances.obsidian", 20) / 100;
        lava = cfg.getDouble("block-chances.lava", 16.67) / 100;
        fireTouch = cfg.getInt("fire-touch", 10);
        radius = cfg.getInt("radius", 4);
    }

    @Override
    protected void prepare(LivingEntity entity) {
        entity.addPotionEffect(FIRE_RESISTANCE);
    }

    @EventHandler
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (fireTouch > 0 && isThis(event.getEntity())) {
            event.getDamager().setFireTicks(fireTouch);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!isThis(event.getEntity()) || changeChance <= 0) return;
        ((LivingEntity) event.getEntity()).removePotionEffect(PotionEffectType.FIRE_RESISTANCE);

        Location start = event.getLocation();
        int cx = start.getBlockX(), cy = start.getBlockY(), cz = start.getBlockZ();
        int radiusSquared = radius * radius;
        start.getWorld().spawnParticle(Particle.FLAME, cx, cy + 1, cz, 20, 0, 0, 0, 2);

        Locations.loop(radius, start, (world, x, y, z) -> {
            int dx = cx - x, dy = cy - y, dz = cz - z;
            if (dx * dx + dy * dy + dz * dz > radiusSquared || !Rng.chance(changeChance)) return;

            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isAir()) {
                if (fire > 0 && !block.getRelative(BlockFace.DOWN).getType().isAir() && Rng.chance(fire)) {
                    block.setType(Material.FIRE);
                }
            } else if (block.getType() != Material.BEDROCK) {
                if (magmaBlock > 0 && Rng.chance(magmaBlock)) {
                    block.setType(Material.MAGMA_BLOCK);
                } else if (obsidian > 0 && Rng.chance(obsidian)) {
                    block.setType(Material.OBSIDIAN);
                } else if (lava > 0 && Rng.chance(lava)) {
                    block.setType(Material.LAVA);
                }
            }
        });
    }

    @Override
    public void tick(LivingEntity entity) {
        entity.getWorld().spawnParticle(Particle.LAVA, entity.getLocation().add(0, 1, 0), 1, 0.3, 0.8, 0.3);
    }
}
