import dev.caveslite.herobrine.EncounterClock;
import java.util.concurrent.atomic.AtomicLong;
public class VerifyEncounterClock {
    static int checks;
    static void check(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
    public static void main(String[] args){
        AtomicLong nanos=new AtomicLong(5_000_000_000L);
        EncounterClock clock=new EncounterClock(nanos::get);
        long start=clock.ticks();check(start>0,"Positive sentinel-safe start");
        long frozenWorldTime=6000; // neither used nor allowed to gate encounter progress
        nanos.addAndGet(35_000_000_000L);
        long fleeStart=clock.ticks();
        check(fleeStart-start==700,"35 seconds combat with world clock frozen");
        check(EncounterClock.secondsToTicks(4)==80,"4 seconds flee is 80 ticks, not 400");
        nanos.addAndGet(3_950_000_000L);
        check(clock.ticks()-fleeStart<EncounterClock.secondsToTicks(4),"Still fleeing at 3.95s");
        nanos.addAndGet(50_000_000L);
        check(clock.ticks()-fleeStart>=EncounterClock.secondsToTicks(4),"Expires at 4s even if movement blocked");
        check(clock.ticks()-start==780,"Total combat + flee = 39 seconds");
        frozenWorldTime=-100000; // simulate /time rolling backward
        nanos.addAndGet(1_000_000_000L);
        check(clock.ticks()-start==800,"Backward world time cannot extend encounter");
        frozenWorldTime=Long.MAX_VALUE;
        check(clock.ticks()-start==800,"Forward world time cannot skip encounter");
        nanos.addAndGet(560_000_000_000L);
        check(clock.ticks()-fleeStart>EncounterClock.secondsToTicks(4),"Ten minute encounter is overdue");
        check(EncounterClock.secondsToTicks(0)==20,"Minimum one second");
        check(EncounterClock.secondsToTicks(-1)==20,"Negative config bounded");
        check(EncounterClock.secondsToTicks(Long.MAX_VALUE)>0,"Large duration does not overflow");
        AtomicLong rollover=new AtomicLong(Long.MAX_VALUE-40_000_000L);
        EncounterClock wrapClock=new EncounterClock(rollover::get);long before=wrapClock.ticks();
        rollover.addAndGet(100_000_000L);
        check(wrapClock.ticks()-before==2,"nanoTime signed rollover handled by elapsed subtraction");
        AtomicLong attackNanos=new AtomicLong(0);
        EncounterClock attackClock=new EncounterClock(attackNanos::get);
        long nextAttack=0,oldNextAttack=0; int hits=0,oldHits=0;
        for(int update=0;update<=18;update++) {
            attackNanos.set(update*200_000_000L);
            if(attackClock.ticks()>=nextAttack){hits++;nextAttack=attackClock.ticks()+24;}
            if(0>=oldNextAttack){oldHits++;oldNextAttack=24;}
        }
        check(hits==4,"Repeated attacks at 24-tick cooldown despite frozen world clock");
        check(oldHits==1,"Reproduces old one-hit failure with frozen world clock");
        System.out.println("PASS: "+checks+" encounter timer regression checks");
    }
}
