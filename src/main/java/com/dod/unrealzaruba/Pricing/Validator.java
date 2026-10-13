package com.dod.unrealzaruba.Pricing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Port of the backend SchematicValidator: the rules a schematic must meet to be accepted. Its box is not bigger than
 * the configured maximum, and it holds no block, item (say, in a chest) or fluid the price list forbids. The messages
 * are the backend ones (Russian, for the player) and must stay word for word the same.
 */
public final class Validator {
    private static final int MAX_LISTED = 10;

    /** Problems is what is wrong, empty if the schematic is acceptable. */
    public record Check(List<String> problems, List<String> forbiddenBlocks, List<String> forbiddenItems,
                        List<String> forbiddenFluids, int[] size, int[] maxSize) {
        public boolean isAllowed() {
            return problems.isEmpty();
        }
    }

    private Validator() {}

    /** Sizes are compared largest to largest, so a build is not refused for lying along another axis than the limit first one. */
    public static Check check(Composition composition, PriceCatalog catalog, int[] maxSize) {
        List<String> problems = new ArrayList<>();

        int[] size = composition.size;
        if (size != null && size.length == 3) {
            int[] have = descending(size);
            int[] limit = descending(maxSize);
            boolean tooBig = false;
            for (int i = 0; i < 3; i++) if (have[i] > limit[i]) tooBig = true;
            if (tooBig) {
                problems.add("Схематик слишком большой: " + size[0] + "x" + size[1] + "x" + size[2]
                        + " блоков, максимум " + maxSize[0] + "x" + maxSize[1] + "x" + maxSize[2]);
            }
        }

        List<String> blocks = forbidden(Pricer.BLOCK, composition.blocks.keySet(), catalog, "запрещённые блоки", problems);
        List<String> items = forbidden(Pricer.ITEM, composition.items.keySet(), catalog,
                "запрещённые предметы (например, в сундуках)", problems);
        List<String> fluids = forbidden(Pricer.FLUID, composition.fluids.keySet(), catalog, "запрещённые жидкости", problems);

        return new Check(problems, blocks, items, fluids, size, maxSize);
    }

    private static int[] descending(int[] values) {
        int[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        for (int i = 0; i < sorted.length / 2; i++) {
            int swap = sorted[i];
            sorted[i] = sorted[sorted.length - 1 - i];
            sorted[sorted.length - 1 - i] = swap;
        }
        return sorted;
    }

    private static List<String> forbidden(String kind, Collection<String> ids, PriceCatalog catalog, String what, List<String> problems) {
        List<String> forbidden = ids.stream().filter(id -> catalog.isForbidden(kind, id)).toList();
        if (!forbidden.isEmpty()) {
            List<String> listed = forbidden.subList(0, Math.min(MAX_LISTED, forbidden.size()));
            problems.add("В схематике есть " + what + ": " + String.join(", ", listed)
                    + (forbidden.size() > MAX_LISTED ? " и ещё " + (forbidden.size() - MAX_LISTED) : ""));
        }
        return List.copyOf(forbidden.subList(0, Math.min(MAX_LISTED, forbidden.size())));
    }
}
