package com.dod.UnrealZaruba.UI;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TimerOverlay {

    public static final IGuiOverlay TIMER_OVERLAY = ((gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int textWidth = font.width("time");
        int xPos = (screenWidth - textWidth) / 2;

        guiGraphics.drawString(font, "time", xPos, 10, 0xFFFFFF, true);
    });
}
