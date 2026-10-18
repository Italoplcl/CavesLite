import dev.caveslite.mobs.defaults.GiantSettings;
import dev.caveslite.util.BodySpace;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.VoxelShape;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

public class VerifyGiantPreview {
    static int checks;
    static final Map<String,List<BoundingBox>> shapes=new HashMap<>();
    static final Set<String> liquids=new HashSet<>();
    static boolean loaded=true;
    static String key(int x,int y,int z){return x+":"+y+":"+z;}
    static void check(boolean condition,String why){checks++;if(!condition)throw new AssertionError(why);}
    static Object proxy(Class<?> type,InvocationHandler handler){return Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler);}
    static World world(){
        return (World)proxy(World.class,(o,m,a)->switch(m.getName()){
            case "getMinHeight" -> -64;
            case "getMaxHeight" -> 320;
            case "isChunkLoaded" -> loaded;
            case "getBlockAt" -> {
                int x=(int)a[0],y=(int)a[1],z=(int)a[2];String key=key(x,y,z);
                VoxelShape shape=(VoxelShape)proxy(VoxelShape.class,(obj,method,args)->switch(method.getName()){
                    case "getBoundingBoxes" -> shapes.getOrDefault(key,List.of());
                    case "overlaps" -> shapes.getOrDefault(key,List.of()).stream().anyMatch(b->b.overlaps((BoundingBox)args[0]));
                    default -> throw new AssertionError(method.getName());
                });
                yield proxy(Block.class,(obj,method,args)->switch(method.getName()){
                    case "isLiquid" -> liquids.contains(key);
                    case "getCollisionShape" -> shape;
                    default -> throw new AssertionError("Unexpected block mutation/read: "+method.getName());
                });
            }
            case "equals" -> o==a[0];
            case "hashCode" -> System.identityHashCode(o);
            default -> throw new AssertionError("Unexpected world access/mutation: "+m.getName());
        });
    }
    static void solid(int x,int y,int z){shapes.put(key(x,y,z),List.of(new BoundingBox(0,0,0,1,1,1)));}
    public static void main(String[] args)throws Exception {
        World world=world();Location feet=new Location(world,1000.5,64,-1000.5);
        check(BodySpace.fits(feet,3.6,12),"Open space away from coordinate origin");
        solid(1001,70,-1001);
        check(!BodySpace.fits(feet,3.6,12),"Upper body obstruction: world-to-local collision translation");
        shapes.clear();solid(1002,64,-1001);
        check(!BodySpace.fits(feet,3.6,12),"Wide body hits narrow passage side wall");
        shapes.clear();solid(1003,64,-1001);
        check(BodySpace.fits(feet,3.6,12),"Wall outside body must not reject route");
        shapes.clear();solid(1000,75,-1001);
        check(!BodySpace.fits(feet,3.6,12),"Low ceiling rejected");
        shapes.clear();solid(1000,76,-1001);
        check(BodySpace.fits(feet,3.6,12),"Exact 12-block ceiling clearance");
        shapes.clear();solid(1000,63,-1001);
        check(BodySpace.fits(feet,3.6,12),"Floor contact is not collision");
        shapes.put(key(1000,63,-1001),List.of(new BoundingBox(.375,0,.375,.625,1.5,.625)));
        check(!BodySpace.fits(feet,3.6,12),"Fence extending above supporting block rejected");
        shapes.clear();liquids.add(key(1000,64,-1001));
        check(!BodySpace.fits(feet,3.6,12),"Lava/water inside body rejected");
        liquids.clear();liquids.add(key(1000,63,-1001));
        check(BodySpace.fits(feet,3.6,12),"Liquid strictly below body does not overlap");
        liquids.clear();loaded=false;check(!BodySpace.fits(feet,3.6,12),"No forced chunk access");loaded=true;
        check(!BodySpace.fits(new Location(world,0,310,0),3.6,12),"World ceiling");
        check(!BodySpace.fits(new Location(world,0,-65,0),3.6,12),"World bottom");
        Location center=BodySpace.navigationCenter(new Location(world,1000,64,-1000),3.6);
        check(center.getX()==1002&&center.getZ()==-998,"Wide native path node center uses width offset");
        center=BodySpace.navigationCenter(new Location(world,0,64,0),0.6);
        check(center.getX()==0.5,"Normal zombie path node center");
        var cfg=YamlConfiguration.loadConfiguration(Path.of(args[0],"src/main/resources/config.yml").toFile());
        var giant=cfg.getConfigurationSection("mobs.giant-zombie");var settings=GiantSettings.load(giant);
        check(settings.chase()&&settings.melee().enabled()&&settings.stomp().enabled()&&settings.quake().enabled(),"All approved movement/attacks enabled");
        check(settings.melee().damage()==6&&settings.stomp().damage()==8&&settings.quake().damage()==6,"Base attack damage");
        check(settings.stomp().warningTicks()==24&&settings.quake().warningTicks()==32,"Telegraphed attacks");
        check(settings.maxMinions()==3&&settings.amount()==2&&settings.maxWaves()==2,"Reinforcement limits");
        check(settings.summonCooldown()==600&&settings.minionLifetime()==900,"Seconds converted to ticks once");
        giant.set("attacks.quake.enabled",false);giant.set("reinforcements.enabled",false);
        settings=GiantSettings.load(giant);check(!settings.quake().enabled()&&!settings.reinforcements(),"Features independently disabled");
        giant.set("movement.speed",Double.NaN);giant.set("attacks.stomp.damage",Double.POSITIVE_INFINITY);
        settings=GiantSettings.load(giant);check(settings.speed()==0.23&&settings.stomp().damage()==8,"Non-finite input falls back safely");
        giant.set("reinforcements.max-active",10000);giant.set("attacks.stomp.radius",10000);
        settings=GiantSettings.load(giant);check(settings.maxMinions()==12&&settings.stomp().radius()==16,"Work bounded under extreme configuration");
        var loot=cfg.getMapList("mobs.giant-zombie.loot.items");
        check(loot.size()==2&&loot.get(0).get("material").equals("ROTTEN_FLESH"),"Customizable loot schema");
        var weights=Map.ofEntries(Map.entry("alpha-spider",1),Map.entry("crying-bat",1),Map.entry("hexed-armor",1),Map.entry("lava-creeper",1),Map.entry("watcher",1),Map.entry("illusioner",1),Map.entry("cave-golem",2),Map.entry("dead-miner",3),Map.entry("hungering-darkness",1),Map.entry("magma-monster",1),Map.entry("mimic",1),Map.entry("smoke-demon",1),Map.entry("tnt-creeper",3),Map.entry("giant-zombie",1),Map.entry("chicken-jockey",1),Map.entry("killer-bunny",1),Map.entry("spider-jockey",2),Map.entry("skeleton-horseman",2));
        Set<String> off=Set.of("alpha-spider","crying-bat","hexed-armor","lava-creeper","watcher","illusioner");
        Set<String> rare=Set.of("hungering-darkness","mimic","giant-zombie");
        for(var entry:weights.entrySet()){
            String path="mobs."+entry.getKey()+".";
            check(cfg.getBoolean(path+"enabled")==!off.contains(entry.getKey()),"Approved enabled state: "+entry.getKey());
            check(cfg.getInt(path+"priority")==entry.getValue(),"Approved weight: "+entry.getKey());
            check(cfg.getInt(path+"cooldown-seconds")== (rare.contains(entry.getKey())?600:120),"Approved cooldown: "+entry.getKey());
            check(cfg.getInt(path+"max-active")== (rare.contains(entry.getKey())?1:2),"Approved cap: "+entry.getKey());
        }
        check(cfg.getInt("mobs.try-chance")==10,"Basic chance 10 percent");
        check(cfg.getBoolean("herobrine.enabled")&&cfg.getInt("herobrine.max-active-per-player")==1,"Herobrine active with per-player cap");
        check(cfg.getInt("herobrine.encounter-interval.min-seconds")==900&&cfg.getInt("herobrine.encounter-interval.max-seconds")==1500,"Herobrine 15–25 minutes");
        var performance=YamlConfiguration.loadConfiguration(Path.of(args[0],"src/main/resources/presets/performance.yml").toFile());
        check(performance.getInt("mobs.try-chance")==5,"Performance lower chance");
        for(String id:weights.keySet())check(performance.getInt("mobs."+id+".max-active")==1,"Performance cap "+id);
        check(Files.readString(Path.of(args[0],"src/main/resources/config.yml")).equals(Files.readString(Path.of(args[0],"src/main/resources/presets/basic.yml"))),"Bundled basic preset matches live default config");
        System.out.println("PASS: "+checks+" Giant geometry/settings, loot schema and approved preset checks");
    }
}
