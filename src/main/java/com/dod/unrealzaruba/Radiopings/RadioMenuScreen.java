package com.dod.unrealzaruba.Radiopings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Counter-Strike style radio menu (Z): eight phrases, buttons or keys 1-8, Esc closes. */
public class RadioMenuScreen extends Screen {
    private static final int BUTTON_W = 170;
    private static final int BUTTON_H = 20;

    protected RadioMenuScreen() {
        super(Component.translatable("radio.unrealzaruba.menu.title"));
    }

    public static void open() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new RadioMenuScreen());
    }

    @Override
    protected void init() {
        RadioPhrase[] phrases = RadioPhrase.values();
        int cols = 2;
        int rows = (phrases.length + cols - 1) / cols;
        int totalW = cols * BUTTON_W + (cols - 1) * 8;
        int totalH = rows * (BUTTON_H + 4);
        int x0 = (width - totalW) / 2;
        int y0 = (height - totalH) / 2;
        for (int i = 0; i < phrases.length; i++) {
            final int index = i;
            int x = x0 + (i % cols) * (BUTTON_W + 8);
            int y = y0 + (i / cols) * (BUTTON_H + 4);
            Component label = Component.literal((i + 1) + ". ")
                    .append(Component.translatable(phrases[i].langKey));
            addRenderableWidget(Button.builder(label, button -> choose(index))
                    .bounds(x, y, BUTTON_W, BUTTON_H)
                    .build());
        }
    }

    private void choose(int index) {
        onClose();
        RadioClient.sendPhrase(index);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_1
                && keyCode <= org.lwjgl.glfw.GLFW.GLFW_KEY_1 + RadioPhrase.values().length - 1) {
            choose(keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
