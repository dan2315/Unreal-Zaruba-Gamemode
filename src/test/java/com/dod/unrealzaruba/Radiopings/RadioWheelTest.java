package com.dod.unrealzaruba.Radiopings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RadioWheelTest {
    @Test
    void cardinalsMapToSectors() {
        // Slice 0 is at the top, the rest go clockwise.
        assertEquals(0, RadioMenuScreen.sectorAt(0, -100, 8, 10));
        assertEquals(2, RadioMenuScreen.sectorAt(100, 0, 8, 10));
        assertEquals(4, RadioMenuScreen.sectorAt(0, 100, 8, 10));
        assertEquals(6, RadioMenuScreen.sectorAt(-100, 0, 8, 10));
    }

    @Test
    void diagonalsMapToSectors() {
        assertEquals(1, RadioMenuScreen.sectorAt(100, -100, 8, 10));
        assertEquals(3, RadioMenuScreen.sectorAt(100, 100, 8, 10));
        assertEquals(5, RadioMenuScreen.sectorAt(-100, 100, 8, 10));
        assertEquals(7, RadioMenuScreen.sectorAt(-100, -100, 8, 10));
    }

    @Test
    void deadzoneReturnsNone() {
        assertEquals(-1, RadioMenuScreen.sectorAt(0, 0, 8, 10));
        assertEquals(-1, RadioMenuScreen.sectorAt(5, 5, 8, 10));
        // Just outside the deadzone still resolves.
        assertEquals(2, RadioMenuScreen.sectorAt(11, 0, 8, 10));
    }

    @Test
    void boundariesBelongToNextSliceClockwise() {
        // Boundary between slice 0 (top) and slice 1 (top-right): 22.5 degrees clockwise from north.
        double a = Math.toRadians(22.5);
        double dx = Math.sin(a) * 100;
        double dy = -Math.cos(a) * 100;
        assertEquals(1, RadioMenuScreen.sectorAt(dx, dy, 8, 10));
    }

    @Test
    void sectorCentresPointOutward() {
        double top = RadioMenuScreen.sectorCentre(0, 8);
        assertEquals(0.0, Math.cos(top), 1e-9);
        assertEquals(-1.0, Math.sin(top), 1e-9);
        double right = RadioMenuScreen.sectorCentre(2, 8);
        assertEquals(1.0, Math.cos(right), 1e-9);
        assertEquals(0.0, Math.sin(right), 1e-9);
    }

    @Test
    void fullCircleOfSectors() {
        // Every 5 degrees lands in some valid slice, neighbours agree at boundaries.
        int prev = -2;
        for (int deg = 0; deg < 360; deg += 5) {
            double a = Math.toRadians(deg - 90.0);
            int s = RadioMenuScreen.sectorAt(Math.cos(a) * 100, Math.sin(a) * 100, 8, 10);
            assertTrue(s >= 0 && s < 8);
            if (prev >= 0) assertTrue(s == prev || s == (prev + 1) % 8);
            prev = s;
        }
    }
}
