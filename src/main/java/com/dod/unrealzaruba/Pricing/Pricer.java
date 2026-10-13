package com.dod.unrealzaruba.Pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Port of the backend SchematicPricer: a composition times the price list. Keep rounding, caps and ordering identical. */
public final class Pricer {
    public static final String BLOCK = "block";
    public static final String ITEM = "item";
    /** The price of a fluid is per bucket (1000 mB). */
    public static final String FLUID = "fluid";

    /** Prices stay far below the database column, whatever a crafted file claims to contain. */
    public static final BigDecimal MAX_TOTAL = new BigDecimal("1000000000000000");
    public static final int MAX_LINES = 200;
    public static final int MAX_UNPRICED = 200;

    private static final BigDecimal PER_MILLIBUCKET = new BigDecimal("0.001");

    public record Line(String kind, String id, long count, BigDecimal unitPrice, BigDecimal subtotal) {}

    public record Unpriced(String kind, String id, long count) {}

    /** All amounts rounded to cents. Lines are the priced entries that cost most first; unpriced counts as 0. */
    public record Breakdown(BigDecimal total, BigDecimal blocks, BigDecimal items, BigDecimal fluids,
                            List<Line> lines, boolean linesTruncated, List<Unpriced> unpriced) {}

    private Pricer() {}

    public static Breakdown price(Composition composition, PriceCatalog catalog) {
        List<Line> lines = new ArrayList<>();
        List<Unpriced> unpriced = new ArrayList<>();

        BigDecimal blocks = sum(BLOCK, composition.blocks, BigDecimal.ONE, catalog, lines, unpriced);
        BigDecimal items = sum(ITEM, composition.items, BigDecimal.ONE, catalog, lines, unpriced);
        // Fluid amounts are millibuckets, the price is per bucket.
        BigDecimal fluids = sum(FLUID, composition.fluids, PER_MILLIBUCKET, catalog, lines, unpriced);

        lines.sort(Comparator.comparing(Line::subtotal).reversed()
                .thenComparing(Line::kind).thenComparing(Line::id));
        unpriced.sort(Comparator.comparingLong(Unpriced::count).reversed().thenComparing(Unpriced::id));

        BigDecimal total = blocks.add(items).add(fluids).min(MAX_TOTAL);
        return new Breakdown(round(total), round(blocks), round(items), round(fluids),
                List.copyOf(lines.subList(0, Math.min(MAX_LINES, lines.size()))), lines.size() > MAX_LINES,
                List.copyOf(unpriced.subList(0, Math.min(MAX_UNPRICED, unpriced.size()))));
    }

    private static BigDecimal sum(String kind, Map<String, Long> counts, BigDecimal unit, PriceCatalog catalog,
                                  List<Line> lines, List<Unpriced> unpriced) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            BigDecimal price = catalog.lookup(kind, entry.getKey());
            if (price == null) {
                unpriced.add(new Unpriced(kind, entry.getKey(), entry.getValue()));
                continue;
            }
            BigDecimal subtotal = BigDecimal.valueOf(entry.getValue()).multiply(price).multiply(unit).min(MAX_TOTAL);
            sum = sum.add(subtotal).min(MAX_TOTAL);
            lines.add(new Line(kind, entry.getKey(), entry.getValue(), price, round(subtotal)));
        }
        return sum;
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
