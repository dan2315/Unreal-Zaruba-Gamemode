package com.dod.unrealzaruba.Pricing;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The backend price list (GET /schematics/rules). A price is found by the exact id ({@code minecraft:stone}), then by
 * the mod wildcard ({@code create:*}), then by the wildcard for everything of that kind ({@code *}); none of them
 * means "unpriced". Port of the backend PriceCatalog: keep the lookup order identical.
 */
public final class PriceCatalog {
    public static final String WILDCARD = "*";

    public record Entry(String kind, String id, BigDecimal price, boolean forbidden) {}

    private final Map<String, Entry> entries = new HashMap<>();

    public PriceCatalog(Collection<Entry> list) {
        for (Entry entry : list) entries.put(key(entry.kind(), entry.id()), entry);
    }

    private static String key(String kind, String id) {
        return kind + '\0' + id;
    }

    public int size() {
        return entries.size();
    }

    /**
     * Whether the block, item or fluid is forbidden. The same lookup order as prices decides, so an exact entry that is
     * not forbidden makes an exception to a forbidden {@code mod:*}.
     */
    public boolean isForbidden(String kind, String id) {
        Entry exact = entries.get(key(kind, id));
        if (exact != null) return exact.forbidden();

        int colon = id.indexOf(':');
        if (colon > 0) {
            Entry namespaced = entries.get(key(kind, id.substring(0, colon) + ":*"));
            if (namespaced != null) return namespaced.forbidden();
        }
        Entry wildcard = entries.get(key(kind, WILDCARD));
        return wildcard != null && wildcard.forbidden();
    }

    /** The price per item or block (per bucket for fluids), or null when nothing in the list matches. */
    public BigDecimal lookup(String kind, String id) {
        Entry exact = entries.get(key(kind, id));
        if (exact != null) return exact.price();

        int colon = id.indexOf(':');
        if (colon > 0) {
            Entry namespaced = entries.get(key(kind, id.substring(0, colon) + ":*"));
            if (namespaced != null) return namespaced.price();
        }
        Entry wildcard = entries.get(key(kind, WILDCARD));
        return wildcard != null ? wildcard.price() : null;
    }
}
