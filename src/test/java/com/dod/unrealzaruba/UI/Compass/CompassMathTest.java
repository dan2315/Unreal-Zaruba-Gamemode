package com.dod.unrealzaruba.UI.Compass;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompassMathTest {
    @Test
    void yawToBearing() {
        assertEquals(180.0, CompassMath.yawToBearing(0f), 1e-9); // South
        assertEquals(270.0, CompassMath.yawToBearing(90f), 1e-9); // West
        assertEquals(0.0, CompassMath.yawToBearing(180f), 1e-9); // North
        assertEquals(90.0, CompassMath.yawToBearing(270f), 1e-9); // East
        assertEquals(0.0, CompassMath.yawToBearing(-180f), 1e-9);
    }

    @Test
    void bearingTo() {
        // North is -Z, East is +X.
        assertEquals(0.0, CompassMath.bearingTo(0, 0, 0, -10), 1e-9);
        assertEquals(90.0, CompassMath.bearingTo(0, 0, 10, 0), 1e-9);
        assertEquals(180.0, CompassMath.bearingTo(0, 0, 0, 10), 1e-9);
        assertEquals(270.0, CompassMath.bearingTo(0, 0, -10, 0), 1e-9);
        assertEquals(45.0, CompassMath.bearingTo(0, 0, 10, -10), 1e-9);
    }

    @Test
    void relativeWraps() {
        assertEquals(10.0, CompassMath.relative(10, 0), 1e-9);
        assertEquals(-10.0, CompassMath.relative(350, 0), 1e-9);
        assertEquals(170.0, CompassMath.relative(0, 190), 1e-9);
        assertEquals(-170.0, CompassMath.relative(190, 0), 1e-9);
    }

    @Test
    void ticksEveryFiveDegrees() {
        // Every 5-degree bearing in range must map inside the strip, monotonic with bearing.
        double center = 182.0;
        double prev = Double.NEGATIVE_INFINITY;
        for (double b = center - 60; b <= center + 60; b += 5) {
            double x = CompassMath.xForBearing(CompassMath.normalize360(b), center, 220, 214, 60);
            assertTrue(x >= 220 - 214 && x <= 220 + 214, "out of strip: " + b);
            assertTrue(x > prev, "not monotonic at " + b);
            prev = x;
        }
    }

    @Test
    void labels() {
        assertEquals("N", CompassMath.labelFor(0));
        assertEquals("E", CompassMath.labelFor(90));
        assertEquals("S", CompassMath.labelFor(180));
        assertEquals("W", CompassMath.labelFor(270));
        assertEquals("NE", CompassMath.labelFor(45));
        assertEquals("SE", CompassMath.labelFor(135));
        assertEquals("105", CompassMath.labelFor(105));
        assertNull(CompassMath.labelFor(107));
        assertNull(CompassMath.labelFor(103));
    }

    @Test
    void cardinals() {
        assertEquals("N", CompassMath.cardinalFor(0));
        assertEquals("N", CompassMath.cardinalFor(359));
        assertEquals("E", CompassMath.cardinalFor(90));
        assertEquals("S", CompassMath.cardinalFor(181));
        assertEquals("SW", CompassMath.cardinalFor(225));
        assertEquals("NW", CompassMath.cardinalFor(315));
    }
}
