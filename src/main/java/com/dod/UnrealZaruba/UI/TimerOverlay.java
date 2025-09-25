package com.dod.unrealzaruba.UI;

import com.dod.unrealzaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.unrealzaruba.utils.Timers.RealTimeTimer;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TimerOverlay implements IGuiOverlay, IGameTimer {
    public boolean isVisible = false;

    public int minutes;
    public int seconds;

    private RealTimeTimer timer;
    private int durationSeconds;
    public static IGuiOverlay INSTANCE = new TimerOverlay();
    public static TimerOverlay OVERLAY_INSTANCE = (TimerOverlay) INSTANCE;


    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();

        if (!isVisible) {
            return;
        }

        if (mc.player == null || mc.level == null) {
            return;
        }

        Font font = mc.font;

        var displayableText = String.format("%02d:%02d", this.minutes, this.seconds);
        int x = screenWidth / 2 - font.width(displayableText) / 2;
        int y = 10;

        guiGraphics.renderItem(new ItemStack(Items.CLOCK), 20, y - 3);
        guiGraphics.drawString(font, displayableText, 40, y, 0xFFFFFF, true);
    }


    @Override
    public void startCountDown(long startTime, int durationSeconds) {
        isVisible = true;
        this.durationSeconds = durationSeconds;
        timer = TimerManager.createRealTimeTimer(durationSeconds, startTime, this::updateTime);
    }

    @Override
    public void stop() {
        isVisible = false;
        timer.dispose(true);
    }

    private void updateTime(int ticks) {
        var remainingSeconds = durationSeconds - ticks / 20;
        this.minutes = remainingSeconds / 60;
        this.seconds = remainingSeconds % 60;
    }
}
