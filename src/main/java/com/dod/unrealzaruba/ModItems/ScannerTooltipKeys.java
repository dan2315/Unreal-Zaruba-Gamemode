package com.dod.unrealzaruba.ModItems;

import net.minecraft.client.gui.screens.Screen;

/** Reads the Shift key for item tooltips. Its own class, so the client-only Screen is loaded only where tooltips are drawn. */
final class ScannerTooltipKeys {
    private ScannerTooltipKeys() {}

    static boolean shiftDown() {
        return Screen.hasShiftDown();
    }
}
