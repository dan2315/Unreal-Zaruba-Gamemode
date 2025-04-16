package com.dod.UnrealZaruba.UI;

import com.dod.UnrealZaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TimerOverlay implements IGuiOverlay, IGameTimer {
    public boolean is_visible = false;

    public int minutes;
    public int seconds;

    public static IGuiOverlay INSTANCE = new TimerOverlay();
    public static TimerOverlay OVERLAY_INSTANCE = (TimerOverlay) INSTANCE;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();

        if (!is_visible) {
            return;
        }

        if (mc.player == null || mc.level == null) {
            return;
        }

        Font font = mc.font;

        var displayable_text = String.format("%02d:%02d", this.minutes, this.seconds);
        int x = screenWidth / 2 - font.width(displayable_text) / 2;
        int y = 10;

        guiGraphics.renderItem(new ItemStack(Items.CLOCK), 20, y - 3);
        guiGraphics.drawString(font, displayable_text, 40, y, 0xFFFFFF, true);
    }

    @Override
    public void setup() {
        UnrealZaruba.LOGGER.info("Засетапился.");
        this.is_visible = true;
    }

    @Override
    public void update(int seconds, int minutes, boolean isVisible) {
        UnrealZaruba.LOGGER.info("Обновился");
        this.seconds = seconds;
        this.minutes = minutes;
        this.is_visible = isVisible;
    }

    @Override
    public void reset() {
        UnrealZaruba.LOGGER.info("Сброс.");
        this.is_visible = false;
    }
}
