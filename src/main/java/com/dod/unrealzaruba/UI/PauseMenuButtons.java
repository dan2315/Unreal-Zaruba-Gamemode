package com.dod.unrealzaruba.UI;

import com.dod.unrealzaruba.Schematics.SchematicsBrowserScreen;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** The Esc menu gets a Schematics button (with its key in square brackets), outlined with a one-pixel brand gradient. */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class PauseMenuButtons {
    private static final int HEIGHT = 20;
    private static final int GAP = 4;

    private PauseMenuButtons() {}

    @SubscribeEvent
    public static void onPauseInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // The width and the left edge of the column of buttons, and where it begins and ends
        int left = Integer.MAX_VALUE, right = Integer.MIN_VALUE, top = Integer.MAX_VALUE, bottom = Integer.MIN_VALUE;
        for (GuiEventListener listener : event.getListenersList()) {
            if (!(listener instanceof Button button)) continue;
            left = Math.min(left, button.getX());
            right = Math.max(right, button.getX() + button.getWidth());
            top = Math.min(top, button.getY());
            bottom = Math.max(bottom, button.getY() + button.getHeight());
        }
        if (left == Integer.MAX_VALUE) return;

        // Above the first button when there is room under the title, otherwise under the last one
        int y = top - HEIGHT - GAP >= 48 ? top - HEIGHT - GAP : bottom + GAP;
        event.addListener(new GradientButton(left, y, right - left, HEIGHT, Component.translatable("menu.unrealzaruba.schematics").append(" [" + TipsOverlay.englishKeyName(com.dod.unrealzaruba.Events.KeyBindings.OPEN_SCHEMATICS) + "]"),
                b -> mc.setScreen(new SchematicsBrowserScreen())));
    }

    /** A button with the usual look, and a one-pixel outline that runs mint, cyan, blue from left to right. */
    private static final class GradientButton extends Button {
        GradientButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            Brand.gradient(g, x, y, w, 1, Brand.MINT, Brand.CYAN, Brand.BLUE);
            Brand.gradient(g, x, y + h - 1, w, 1, Brand.MINT, Brand.CYAN, Brand.BLUE);
            g.fill(x, y + 1, x + 1, y + h - 1, Brand.MINT);
            g.fill(x + w - 1, y + 1, x + w, y + h - 1, Brand.BLUE);
        }
    }
}
