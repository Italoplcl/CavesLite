package dev.caveslite.herobrine;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.BiPredicate;

/** Bounded ground routing for a mannequin, which has no vanilla mob navigation. */
public final class GroundRoute {
    public record Cell(int x, int y, int z) {
        double distance(Cell other) {
            return Math.abs(x-other.x)+Math.abs(z-other.z)+1.5*Math.abs(y-other.y);
        }
    }
    private record Visit(Cell cell, double cost, double score) {}
    public static Cell next(Cell start, Cell goal, Predicate<Cell> safe, BiPredicate<Cell,Cell> clear) {
        var open = new PriorityQueue<Visit>(Comparator.comparingDouble(Visit::score));
        Map<Cell, Double> costs = new HashMap<>();
        Map<Cell, Cell> parents = new HashMap<>();
        costs.put(start, 0d); open.add(new Visit(start,0,start.distance(goal)));
        Cell best=start;
        int visited=0;
        while (!open.isEmpty() && visited++ < 512) {
            Visit visit=open.poll(); Cell current=visit.cell();
            if (visit.cost()>costs.getOrDefault(current,Double.MAX_VALUE)) continue;
            if (current.distance(goal)<best.distance(goal)) best=current;
            if (current.equals(goal)) { best=current; break; }
            for (int[] direction : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                for (int dy : new int[]{0,1,-1,-2,-3}) {
                    Cell next=new Cell(current.x()+direction[0],current.y()+dy,current.z()+direction[1]);
                    if (Math.abs(next.x()-start.x())>24 || Math.abs(next.z()-start.z())>24
                            || Math.abs(next.y()-start.y())>24 || !safe.test(next) || !clear.test(current,next)) continue;
                    double cost=visit.cost()+1+Math.abs(dy)*0.5;
                    if (cost>=costs.getOrDefault(next,Double.MAX_VALUE)) continue;
                    costs.put(next,cost); parents.put(next,current);
                    open.add(new Visit(next,cost,cost+next.distance(goal)));
                }
            }
        }
        if (best.equals(start)) return null;
        while (!parents.get(best).equals(start)) best=parents.get(best);
        return best;
    }
}
