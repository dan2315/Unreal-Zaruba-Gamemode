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

        // Right under "Back to game", as wide as it is; the buttons below it move down to make room
        Button back = null;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button button && button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents key
                    && key.getKey().equals("menu.returnToGame")) {
                back = button;
                break;
            }
        }
        if (back == null) return;
        int y = back.getY() + back.getHeight() + GAP;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button button && button != back && button.getY() >= y - GAP) button.setY(button.getY() + HEIGHT + GAP);
        }
        event.addListener(new GradientButton(back.getX(), y, back.getWidth(), HEIGHT,
                Component.translatable("menu.unrealzaruba.schematics").append(" [" + TipsOverlay.englishKeyName(com.dod.unrealzaruba.Events.KeyBindings.OPEN_SCHEMATICS) + "]"),
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
