package dev.caveslite.herobrine;

import java.util.function.LongSupplier;

/** Elapsed encounter time, unaffected by world clocks, daylight cycles or /time. */
public final class EncounterClock {
    private final LongSupplier nanos;
    private final long origin;

    public EncounterClock(LongSupplier nanos) {
        this.nanos = nanos;
        origin = nanos.getAsLong();
    }

    public long ticks() {
        return 1L + (nanos.getAsLong() - origin) / 50_000_000L;
    }

    public static long secondsToTicks(long seconds) {
        // Bound durations to avoid overflow from malformed configuration.
        return Math.max(1L, Math.min(seconds, Long.MAX_VALUE / 20L)) * 20L;
    }
}
