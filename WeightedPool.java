package dev.caveslite.util;

import java.util.ArrayList;
import java.util.List;

/** Simple weighted collection: an element with weight N appears N times. */
public final class WeightedPool<T> {
    private final List<T> elements = new ArrayList<>();

    public void add(T element, int weight) {
        for (int i = 0; i < weight; i++) {
            elements.add(element);
        }
    }

    public T next() {
        return elements.get(Rng.nextInt(elements.size()));
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
