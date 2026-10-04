package com.dod.unrealzaruba.UI;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * Adapts the old Forge IGuiOverlay render signature to NeoForge GUI layers.
 */
public interface ZarubaOverlay extends LayeredDraw.Layer {
    void render(Gui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight);

    @Override
    default void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }
        render(minecraft.gui, guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false),
                guiGraphics.guiWidth(), guiGraphics.guiHeight());
    }
}
