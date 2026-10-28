package com.dod.unrealzaruba.Radiopings;

import java.util.List;

import com.dod.unrealzaruba.UI.UiFont;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Counter-Strike style radio wheel: hold the key, point the mouse at a slice, let go (or click, or press
 * 1-8) to send the phrase to the team. Releasing the key over the empty centre cancels (so a quick tap sends
 * nothing). Esc cancels too, the screen just closes and the tick handler has nothing to confirm.
 */
public class RadioMenuScreen extends Screen {
    private static final float RADIUS_FRACTION = 0.33f;
    private static final float INNER_FRACTION = 0.34f;
    private static final float LABEL_SCALE = 0.7f;
    private static final long TAP_MS = 150;

    private static final int DIM = 0x99100F12;
    private static final int BORDER = 0xFF3A3842;
    private static final int MINT = 0xFF00FFA3;
    private static final int TEXT = 0xFFF2F5F3;
    private static final int DETAIL = 0xFFB9BDC2;
    private static final int MUTE = 0xFF7C828A;

    private double mouseX = -1;
    private double mouseY = -1;
    private final long openedAt = System.currentTimeMillis();

    protected RadioMenuScreen() {
        super(Component.translatable("radio.unrealzaruba.menu.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new RadioMenuScreen());
    }

    /**
     * Which slice the cursor points at, or -1 inside the dead centre.
     * Slice 0 is at the top, the rest go clockwise, like a compass rose.
     */
    public static int sectorAt(double dx, double dy, int count, double deadzone) {
        if (Math.hypot(dx, dy) < deadzone) return -1;
        double angle = Math.toDegrees(Math.atan2(dy, dx)); // 0 = east, clockwise (y down)
        double sector = 360.0 / count;
        double shifted = (angle + 90.0 + sector / 2.0) % 360.0;
        if (shifted < 0) shifted += 360.0;
        return (int) (shifted / sector) % count;
    }

    /** Centre angle of a slice in screen radians, 0 = east, clockwise. */
    public static double sectorCentre(int index, int count) {
        return Math.toRadians(index * (360.0 / count) - 90.0);
    }

    public int hovered() {
        if (mouseX < 0) return -1;
        double r = Math.min(width, height) * RADIUS_FRACTION;
        return sectorAt(mouseX - width / 2.0, mouseY - height / 2.0, RadioPhrase.values().length, r * INNER_FRACTION);
    }

    /** Confirm the hovered slice; a tap (too fast) or the empty centre cancels. */
    public void confirmHovered() {
        int index = System.currentTimeMillis() - openedAt < TAP_MS ? -1 : hovered();
        Minecraft.getInstance().setScreen(null);
        if (index >= 0) RadioClient.sendPhrase(index);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == com.dod.unrealzaruba.Events.KeyBindings.RADIO_MENU.getKey().getValue()) {
            confirmHovered();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void choose(int index) {
        Minecraft.getInstance().setScreen(null);
        RadioClient.sendPhrase(index);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            int index = hoveredAt(x, y);
            if (index >= 0) choose(index);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    private int hoveredAt(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
        return hovered();
    }

    @Override
    public void mouseMoved(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
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
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Без ванильного фона: Screen.render() рисует его поверх кастомных слоёв,
        // и панорамный блюр оказывается над колесом, а не под ним. Затемнение рисуем сами.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        int cx = width / 2;
        int cy = height / 2;
        double radius = Math.min(width, height) * RADIUS_FRACTION;
        int hovered = hovered();

        g.fill(0, 0, width, height, DIM);

        RadioPhrase[] phrases = RadioPhrase.values();
        Font font = UiFont.get();
        double labelR = radius + 22;
        for (int i = 0; i < phrases.length; i++) {
            double a = sectorCentre(i, phrases.length);
            int lx = cx + (int) (Math.cos(a) * labelR);
            int ly = cy + (int) (Math.sin(a) * labelR);
            boolean hot = i == hovered;
            Component name = Component.translatable(phrases[i].langKey);
            drawItemLabel(g, font, lx, ly, i, name, phrases[i].glyph, phrases[i].glyphColor, hot);
        }

        if (hovered >= 0) {
            Component name = Component.translatable(phrases[hovered].langKey).withStyle(UiFont.bold());
            com.dod.unrealzaruba.UI.HudStyle.text(g, font, name, cx - com.dod.unrealzaruba.UI.HudStyle.width(font, name.getString(), 1.2f) / 2, cy - (int) (font.lineHeight * 1.2f / 2), TEXT, 1.2f);
        }
        Component hint = Component.translatable("radio.unrealzaruba.wheel.hint");
        com.dod.unrealzaruba.UI.HudStyle.text(g, font, hint, (width - com.dod.unrealzaruba.UI.HudStyle.width(font, hint.getString(), 0.8f)) / 2, height - 28, DETAIL, 0.8f);
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** One centred line per slice: phrase glyph, number, phrase name. */
    private static void drawItemLabel(GuiGraphics g, Font font, int lx, int ly, int index, Component name, PingGlyph glyph, int glyphColor, boolean hot) {
        float s = LABEL_SCALE;
        String num = (index + 1) + ". ";
        int nameW = com.dod.unrealzaruba.UI.HudStyle.width(font, name.getString(), s);
        int numW = com.dod.unrealzaruba.UI.HudStyle.width(font, num, s);
        int glyphW = 12;
        int gap = Math.round(4 * s);
        int totalW = glyphW + gap + numW + gap + nameW;
        int y = ly - Math.round(font.lineHeight * s / 2f);
        int x = lx - totalW / 2;
        glyph.draw(g, x + glyphW / 2, ly, 5, glyphColor);
        x += glyphW + gap;
        com.dod.unrealzaruba.UI.HudStyle.text(g, font, num.trim(), x, y, hot ? DETAIL : MUTE, s);
        x += numW + gap;
        com.dod.unrealzaruba.UI.HudStyle.text(g, font, name, x, y, hot ? MINT : TEXT, s);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
