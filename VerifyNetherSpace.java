package dev.caveslite.mobs;
import java.lang.reflect.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.util.*;

public class VerifyNetherSpace {
    static int checks;
    static Object proxy(Class<?> type, InvocationHandler handler) { return Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler); }
    static void run(String id, double y, boolean collision, boolean liquid, boolean loaded, boolean expected) {
        VoxelShape shape=(VoxelShape)proxy(VoxelShape.class,(o,m,a)-> m.getName().equals("overlaps") ? collision : java.util.List.of());
        Block block=(Block)proxy(Block.class,(o,m,a)->switch(m.getName()){case "isLiquid" -> liquid;case "getCollisionShape" -> shape;default -> throw new UnsupportedOperationException(m.getName());});
        World world=(World)proxy(World.class,(o,m,a)->switch(m.getName()){case "getMinHeight" -> 0;case "getMaxHeight" -> 256;case "isChunkLoaded" -> loaded;case "getBlockAt" -> block;default -> throw new UnsupportedOperationException(m.getName());});
        CustomMob mob=(CustomMob)proxy(CustomMob.class,(o,m,a)->id);
        boolean actual=NetherSpawns.hasSpace(mob,new Location(world,0.5,y,0.5));
        checks++;if(actual!=expected)throw new AssertionError(id+" at "+y+": "+actual);
    }
    public static void main(String[] args) {
        run("giant-zombie",100,false,false,true,true); // above Overworld global ceiling
        run("giant-zombie",20,false,false,true,true); // below Giant Overworld minimum
        run("giant-zombie",250,false,false,true,false); // world ceiling
        run("giant-zombie",100,true,false,true,false); // obstruction
        run("giant-zombie",100,false,true,true,false); // lava
        run("giant-zombie",100,false,false,false,false); // no forced chunk loads
        run("dead-miner",255,false,false,true,false);
        run("dead-miner",254,false,false,true,true);
        System.out.println("PASS: "+checks+" Nether physical-space checks");
    }
}
