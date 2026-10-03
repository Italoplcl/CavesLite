package dev.caveslite.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Rng {
    private Rng() {}

    public static <T> T randomElement(List<T> list) {
        return list.get(nextInt(list.size()));
    }

    public static boolean chance(double v) {
        return nextDouble() < v;
    }

    public static boolean nextBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    public static int nextInt(int max) {
        return ThreadLocalRandom.current().nextInt(max);
    }

    public static double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    public static double nextDouble(double max) {
        return ThreadLocalRandom.current().nextDouble(max);
    }

    public static double nextDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
}
