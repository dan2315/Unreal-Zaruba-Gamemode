package com.dod.unrealzaruba.Pricing;

import java.util.Map;
import java.util.TreeMap;

/**
 * What a build is made of: block id to count, item id to count (stacks inside blocks), fluid id to millibuckets, and the
 * size of the box around its non-air blocks. Same meaning as the backend SchematicComposition; the maps are ordered by
 * id (ordinal) like its SortedDictionaries.
 */
public final class Composition {
    /** Counts stay below this, so multiplying nested stacks cannot overflow. */
    public static final long COUNT_CAP = 1L << 50;

    public final Map<String, Long> blocks = new TreeMap<>();
    public final Map<String, Long> items = new TreeMap<>();
    public final Map<String, Long> fluids = new TreeMap<>();
    /** x, y, z; null for an empty build. */
    public int[] size;

    public static void add(Map<String, Long> counts, String id, long count) {
        if (count <= 0) return;
        long sum = counts.getOrDefault(id, 0L) + count;
        counts.put(id, Math.min(COUNT_CAP, sum));
    }
}
