package com.dod.unrealzaruba.Pricing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** The GET /schematics/rules document, in the shape the backend documents, as the mod reads it. */
class RulesParseTests {
    private static final String DOCUMENT = """
            { "rulesVersion": "a1b2c3d4e5f60718", "scanner": 6, "maxSize": [30, 20, 25],
              "limits": { "fluidBucketMb": 1000, "maxTotal": 1000000000000000, "countCap": 1125899906842624, "maxLines": 200, "maxListedForbidden": 10 },
              "updatedAt": "2026-10-13T12:00:00Z",
              "prices": [
                { "kind": "block", "id": "minecraft:stone", "price": 2.5, "forbidden": false },
                { "kind": "block", "id": "create:*", "price": 0.1234, "forbidden": false },
                { "kind": "item", "id": "minecraft:tnt", "price": 0, "forbidden": true } ] }
            """;

    @Test
    void readsTheDocument() {
        RulesService.Rules rules = RulesService.parse(DOCUMENT);
        assertEquals("a1b2c3d4e5f60718", rules.version());
        assertEquals(6, rules.scanner());
        assertArrayEquals(new int[]{30, 20, 25}, rules.maxSize());
        assertEquals(3, rules.catalog().size());
        assertEquals(0, new BigDecimal("2.5").compareTo(rules.catalog().lookup("block", "minecraft:stone")));
        // Prices keep their four decimals (no double rounding)
        assertEquals(new BigDecimal("0.1234"), rules.catalog().lookup("block", "create:shaft"));
        assertTrue(rules.catalog().isForbidden("item", "minecraft:tnt"));
        assertEquals(null, rules.catalog().lookup("item", "minecraft:stone"));
    }
}
