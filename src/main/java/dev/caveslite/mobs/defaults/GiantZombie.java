package dev.caveslite.mobs.defaults;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import dev.caveslite.herobrine.EncounterClock;
import dev.caveslite.mobs.CustomMob;
import dev.caveslite.mobs.MobBase;
import dev.caveslite.util.BodySpace;
import dev.caveslite.util.Text;
import dev.caveslite.util.Utils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Plugin-owned giant navigation and physical attacks. Never changes terrain. */
public final class GiantZombie extends MobBase implements CustomMob.Ticking, CustomMob.Cleanup, Listener {
    private static final NamespacedKey OWNER = new NamespacedKey("caveslite", "giant-owner");
    private static final NamespacedKey WAVES = new NamespacedKey("caveslite", "giant-waves");
    private static final NamespacedKey EXPIRES = new NamespacedKey("caveslite", "giant-minion-expires");
    private final Plugin plugin;
    private final EncounterClock clock = new EncounterClock(System::nanoTime);
    private final Map<UUID, State> states = new HashMap<>();
    private final Map<UUID, UUID> minions = new HashMap<>();
    private GiantSettings settings;
    private ConfigurationSection config;
    private int minY, maxY;

    private static final class State {
        long nextPath, basic, stomp, quake, summon, ready;
        int waves;
        boolean quaking;
        double waveRadius;
        Location center;
        GiantSettings.Attack pending;
        final Set<UUID> waveHits = new HashSet<>();
    }

    public GiantZombie(Plugin plugin) { super(EntityType.GIANT,"giant-zombie",1,80d,"&fGiant Zombie"); this.plugin=plugin; }

    @Override protected void configure(ConfigurationSection cfg) {
        config=cfg; minY=cfg.getInt("spawn-y-min",80); maxY=cfg.getInt("spawn-y-max",320);
        settings=GiantSettings.load(cfg);
        states.clear(); // Existing entities receive fresh validated goals/settings on their next update.
    }
    @Override public boolean usesGlobalYRange() { return false; }
    @Override public boolean naturalContextAllowed(Location loc) {
        long time=loc.getWorld().getTime();
        return loc.getWorld().getEnvironment()==World.Environment.NORMAL && time>=13000 && time<=23000
                && loc.getBlockY()>=minY && loc.getBlockY()<=maxY && loc.getBlock().getLightFromSky()>0;
    }
    @Override public boolean canSpawn(Location loc) { return BodySpace.fits(loc,3.6,12); }

    @Override protected void prepare(LivingEntity entity) {
        configureBody((Mob)entity);
    }

    private State configureBody(Mob mob) {
        Bukkit.getMobGoals().removeAllGoals(mob); // Only this tagged giant, never ordinary giants.
        mob.setAI(true); mob.setAware(true);
        set(mob,Attribute.MOVEMENT_SPEED,settings.speed());
        set(mob,Attribute.FOLLOW_RANGE,settings.followRange());
        set(mob,Attribute.STEP_HEIGHT,1);
        set(mob,Attribute.KNOCKBACK_RESISTANCE,0.8);
        set(mob,Attribute.ATTACK_DAMAGE,0); // Avoid a second native melee attack.
        var nav=mob.getPathfinder(); nav.setCanOpenDoors(false); nav.setCanPassDoors(false); nav.setCanFloat(false);
        Bukkit.getMobGoals().addGoal(mob,0,new Goal<Mob>() {
            public boolean shouldActivate() { return mob.isValid(); }
            public boolean shouldStayActive() { return mob.isValid(); }
            public GoalKey<Mob> getKey() { return GoalKey.of(Mob.class,new NamespacedKey(plugin,"giant-navigation")); }
            public EnumSet<GoalType> getTypes() { return EnumSet.of(GoalType.MOVE,GoalType.LOOK); }
        });
        State state=new State(); state.waves=mob.getPersistentDataContainer().getOrDefault(WAVES,PersistentDataType.INTEGER,0); long now=clock.ticks();
        state.stomp=now+settings.stomp().cooldownTicks(); state.quake=now+settings.quake().cooldownTicks();
        state.summon=now+settings.summonCooldown();
        states.put(mob.getUniqueId(),state); return state;
    }
    private static void set(LivingEntity mob,Attribute attribute,double value) {
        var instance=mob.getAttribute(attribute); if(instance!=null)instance.setBaseValue(value);
    }
    private boolean eligible(Player player) {
        return player.isOnline() && !player.isDead()
                && (player.getGameMode()==GameMode.SURVIVAL || player.getGameMode()==GameMode.ADVENTURE);
    }

    @Override public void tick(LivingEntity entity) {
        if (!(entity instanceof Mob mob))return;

        State state=states.get(entity.getUniqueId()); if(state==null)state=configureBody(mob);
        long now=clock.ticks();
        Player target=entity.getWorld().getPlayers().stream().filter(this::eligible)
                .filter(p->p.getLocation().distanceSquared(entity.getLocation())<=settings.followRange()*settings.followRange())
                .min(Comparator.comparingDouble(p->p.getLocation().distanceSquared(entity.getLocation()))).orElse(null);
        if(target==null){mob.getPathfinder().stopPathfinding();state.pending=null;state.quaking=false;state.center=null;return;}
        mob.lookAt(target,30,30);
        if(state.quaking){mob.getPathfinder().stopPathfinding();wave(mob,state);return;}
        if(state.pending!=null){
            mob.getPathfinder().stopPathfinding();
            if(now>=state.ready){
                GiantSettings.Attack attack=state.pending; state.pending=null;
                state.center=entity.getLocation().clone();
                sound(state.center,settings.impactSound());
                if(attack==settings.quake()) {state.quaking=true;state.waveRadius=0;state.waveHits.clear();wave(mob,state);}
                else if (attack==settings.melee()) {if(inArea(mob,target,state.center,attack.radius()))hit(mob,target,attack);}
                else {ring(state.center,attack.radius());for(Player p:entity.getWorld().getPlayers())if(inArea(mob,p,state.center,attack.radius()))hit(mob,p,attack);}
            }
            return;
        }
        double distance=entity.getLocation().distance(target.getLocation());
        if(settings.quake().enabled() && now>=state.quake && distance<=settings.quake().radius()) {
            state.quake=now+settings.quake().cooldownTicks();warn(mob,state,settings.quake(),now);return;
        }
        if(settings.stomp().enabled() && now>=state.stomp && distance<=settings.stomp().radius()) {
            state.stomp=now+settings.stomp().cooldownTicks();warn(mob,state,settings.stomp(),now);return;
        }
        if(settings.melee().enabled() && now>=state.basic && inArea(mob,target,entity.getLocation(),settings.melee().radius())) {
            state.basic=now+settings.melee().cooldownTicks();mob.swingMainHand();
            if(settings.melee().warningTicks()>0){warn(mob,state,settings.melee(),now);return;}
            hit(mob,target,settings.melee());
        }
        if(settings.chase() && now>=state.nextPath) {
            state.nextPath=now+settings.repathTicks();
            navigate(mob,target);
        } else if(!settings.chase())mob.getPathfinder().stopPathfinding();
        if(settings.reinforcements() && now>=state.summon && state.waves<settings.maxWaves()) {
            state.summon=now+settings.summonCooldown();
            if(summon(mob,target)>0){state.waves++;mob.getPersistentDataContainer().set(WAVES,PersistentDataType.INTEGER,state.waves);sound(entity.getLocation(),settings.warningSound());}
        }
    }

    private void navigate(Mob mob,Player target) {
        var path=mob.getPathfinder().findPath(target,1);
        if(path==null || path.getPoints().isEmpty()){mob.getPathfinder().stopPathfinding();return;}
        double width=mob.getWidth(),height=mob.getHeight();
        for(Location point:path.getPoints())if(!BodySpace.fits(BodySpace.navigationCenter(point,width),width,height)){
            mob.getPathfinder().stopPathfinding();return;
        }
        // Navigation handles collisions, step height and falling; no teleporting through obstacles.
        mob.getPathfinder().moveTo(path,1);
    }

    private boolean inArea(Mob giant,Player target,Location center,double radius) {
        if(!eligible(target) || Math.abs(target.getLocation().getY()-center.getY())>2.5 || !giant.hasLineOfSight(target))return false;
        double x=target.getLocation().getX()-center.getX(),z=target.getLocation().getZ()-center.getZ();
        if(x*x+z*z>radius*radius)return false;
        Location from=center.clone().add(0,0.8,0),to=target.getLocation().clone().add(0,0.8,0);
        Vector line=to.toVector().subtract(from.toVector());double distance=line.length();
        return distance<0.01 || center.getWorld().rayTraceBlocks(from,line.normalize(),distance,FluidCollisionMode.NEVER,true)==null;
    }
    private void hit(Mob giant,Player target,GiantSettings.Attack attack) {
        double before=target.getHealth()+target.getAbsorptionAmount();
        target.damage(attack.damage(),DamageSource.builder(DamageType.MOB_ATTACK).withCausingEntity(giant).withDirectEntity(giant).build());
        if(target.getHealth()+target.getAbsorptionAmount()>=before || attack.knockback()<=0)return;
        Vector push=target.getLocation().toVector().subtract(giant.getLocation().toVector()).setY(0);
        if(push.lengthSquared()<0.001)push=new Vector(1,0,0);
        target.setVelocity(push.normalize().multiply(attack.knockback()).setY(0.25));
    }
    private void warn(Mob mob,State state,GiantSettings.Attack attack,long now) {
        mob.getPathfinder().stopPathfinding(); state.pending=attack;state.ready=now+attack.warningTicks();
        sound(mob.getLocation(),settings.warningSound());ring(mob.getLocation(),attack.radius());
    }
    private void wave(Mob mob,State state) {
        state.waveRadius+=settings.waveStep();ring(state.center,Math.min(state.waveRadius,settings.quake().radius()));
        for(Player target:mob.getWorld().getPlayers()) {
            if(state.waveHits.contains(target.getUniqueId()) || !target.isOnGround()
                    || !inArea(mob,target,state.center,Math.min(state.waveRadius,settings.quake().radius())))continue;
            double d=target.getLocation().toVector().setY(0).distance(state.center.toVector().setY(0));
            if(d<state.waveRadius-settings.waveStep()-0.5)continue;
            state.waveHits.add(target.getUniqueId());hit(mob,target,settings.quake());
        }
        if(state.waveRadius>=settings.quake().radius()){state.quaking=false;state.center=null;state.waveHits.clear();}
    }
    private void ring(Location center,double radius) {
        if(!config.getBoolean("effects.particles",true))return;
        for(int i=0;i<32;i++) {
            double angle=i*Math.PI/16;Location point=center.clone().add(Math.cos(angle)*radius,0.1,Math.sin(angle)*radius);
            if(center.getWorld().isChunkLoaded(point.getBlockX()>>4,point.getBlockZ()>>4))
                center.getWorld().spawnParticle(Particle.SMOKE,point,1,0.02,0.03,0.02,0);
        }
    }
    private void sound(Location loc,String sound) {
        if(sound!=null&&!sound.isBlank())loc.getWorld().playSound(loc,sound,SoundCategory.HOSTILE,settings.volume(),settings.pitch());
    }

    private int summon(Mob giant,Player target) {
        long alive=minions.values().stream().filter(giant.getUniqueId()::equals).count();int made=0;
        for(int i=0;i<settings.amount() && alive+made<settings.maxMinions();i++) {
            for(int attempt=0;attempt<12;attempt++) {
                double angle=ThreadLocalRandom.current().nextDouble(Math.PI*2);
                Location spot=giant.getLocation().clone().add(Math.cos(angle)*3.5,0,Math.sin(angle)*3.5);
                if(!BodySpace.fits(spot,0.6,1.95))continue;
                var floor=spot.getBlock().getRelative(0,-1,0);
                if(!floor.getType().isSolid() || floor.getType()==Material.MAGMA_BLOCK)continue;
                Zombie zombie=spot.getWorld().spawn(spot,Zombie.class,z->{
                    z.setAdult();z.setCanBreakDoors(false);z.setCanPickupItems(false);z.setRemoveWhenFarAway(true);
                    Utils.setMaxHealth(z,settings.minionHealth());set(z,Attribute.SPAWN_REINFORCEMENTS,0);
                    z.getPersistentDataContainer().set(OWNER,PersistentDataType.STRING,giant.getUniqueId().toString());
                    z.getPersistentDataContainer().set(EXPIRES,PersistentDataType.LONG,System.currentTimeMillis()+settings.minionLifetime()*50L);
                });
                zombie.setTarget(target);minions.put(zombie.getUniqueId(),giant.getUniqueId());made++;break;
            }
        }
        return made;
    }
    public void maintenance() {
        sweepMinions();
        states.keySet().removeIf(id -> Bukkit.getEntity(id)==null);
    }
    private void sweepMinions() {
        Iterator<Map.Entry<UUID,UUID>> it=minions.entrySet().iterator();
        while(it.hasNext()) {
            var entry=it.next(); Entity entity=Bukkit.getEntity(entry.getKey());Entity owner=Bukkit.getEntity(entry.getValue());
            if(entity==null || !entity.isValid()){it.remove();continue;}
            long expires=entity.getPersistentDataContainer().getOrDefault(EXPIRES,PersistentDataType.LONG,0L);
            if(owner==null || !owner.isValid() || System.currentTimeMillis()>=expires){entity.remove();it.remove();}
        }
    }
    private void removeMinions(UUID owner) {
        minions.entrySet().removeIf(entry->{
            if(!entry.getValue().equals(owner))return false;
            Entity entity=Bukkit.getEntity(entry.getKey());if(entity!=null)entity.remove();return true;
        });
    }
    @EventHandler public void onLoad(EntitiesLoadEvent event) {
        for(Entity entity:event.getEntities()) {
            String owner=entity.getPersistentDataContainer().get(OWNER,PersistentDataType.STRING);
            if(owner==null)continue;
            try {minions.put(entity.getUniqueId(),UUID.fromString(owner));}
            catch(IllegalArgumentException ex){entity.remove();}
        }
        sweepMinions();
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void onDeath(EntityDeathEvent event) {
        LivingEntity dead=event.getEntity();
        if(dead.getPersistentDataContainer().has(OWNER,PersistentDataType.STRING)){
            event.getDrops().clear();event.setDroppedExp(0);minions.remove(dead.getUniqueId());return;
        }
        if(!isThis(dead))return;
        states.remove(dead.getUniqueId());removeMinions(dead.getUniqueId());
        if(!config.getBoolean("loot.enabled",true))return;
        if(config.getBoolean("loot.replace-vanilla",false))event.getDrops().clear();
        event.setDroppedExp(Math.max(0,Math.min(10000,config.getInt("loot.experience",30))));
        for(Map<?,?> item:config.getMapList("loot.items")) {
            Material material=Material.matchMaterial(String.valueOf(item.get("material")));
            if(material==null || !material.isItem())continue;
            double chance=item.get("chance") instanceof Number n?GiantSettings.bounded(n.doubleValue(),100,0,100):100;
            if(ThreadLocalRandom.current().nextDouble(100)>=chance)continue;
            int min=item.get("min") instanceof Number n?n.intValue():1;
            int max=item.get("max") instanceof Number n?n.intValue():min;
            min=Math.max(1,Math.min(material.getMaxStackSize(),min));max=Math.max(min,Math.min(material.getMaxStackSize(),max));
            ItemStack drop=new ItemStack(material,ThreadLocalRandom.current().nextInt(min,max+1));
            if(item.get("name") instanceof String name){var meta=drop.getItemMeta();meta.displayName(Text.legacy(name));drop.setItemMeta(meta);}
            event.getDrops().add(drop);
        }
    }
    @Override public int cleanupArtifacts() {
        int count=0;for(UUID id:minions.keySet()){Entity entity=Bukkit.getEntity(id);if(entity!=null){entity.remove();count++;}}
        minions.clear();states.clear();return count;
    }
}
