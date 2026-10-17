package com.dod.unrealzaruba.UI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** A horizontal slider for a whole amount of money, in the Unreal Frontiers look. */
public final class AmountSlider {
    private static final int TRACK_HEIGHT = 4;
    private static final int KNOB_WIDTH = 6;
    private static final int KNOB_HEIGHT = 12;

    private int min;
    private int max;
    private int value;
    private boolean dragging;
    private int x;
    private int y;
    private int width;

    /** New limits; the value stays where it was when it still fits, otherwise it moves to the nearest end. */
    public void range(int min, int max) {
        this.min = min;
        this.max = Math.max(min, max);
        this.value = Mth.clamp(value, this.min, this.max);
    }

    public int value() {
        return value;
    }

    public void setValue(int value) {
        this.value = Mth.clamp(value, min, max);
    }

    public boolean usable() {
        return max > min;
    }

    /** Draws the track and the knob; {@code y} is the top of the track's row, which is KNOB_HEIGHT tall. */
    public void render(GuiGraphics g, int x, int y, int width, int mouseX, int mouseY, boolean enabled) {
        this.x = x;
        this.y = y;
        this.width = width;
        int trackY = y + (KNOB_HEIGHT - TRACK_HEIGHT) / 2;
        g.fill(x, trackY, x + width, trackY + TRACK_HEIGHT, Brand.BAR_TRACK);
        int knobX = knobX();
        if (knobX > x) {
            Brand.gradient(g, x, trackY, knobX - x, TRACK_HEIGHT, Brand.MINT_DIM, Brand.CYAN_DIM, Brand.BLUE);
        }
        boolean hover = enabled && (dragging || over(mouseX, mouseY));
        int knob = !enabled ? Brand.TEXT_MUTE : hover ? Brand.MINT : Brand.TEXT;
        g.fill(knobX - KNOB_WIDTH / 2, y, knobX + KNOB_WIDTH / 2, y + KNOB_HEIGHT, knob);
        g.renderOutline(knobX - KNOB_WIDTH / 2, y, KNOB_WIDTH, KNOB_HEIGHT, Brand.WINDOW);
    }

    private int knobX() {
        if (max <= min) return x + width;
        return x + Math.round(width * ((value - min) / (float) (max - min)));
    }

    private boolean over(double mouseX, double mouseY) {
        return mouseX >= x - KNOB_WIDTH && mouseX <= x + width + KNOB_WIDTH && mouseY >= y - 4 && mouseY <= y + KNOB_HEIGHT + 4;
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!usable() || !over(mouseX, mouseY)) return false;
        dragging = true;
        setFromMouse(mouseX);
        return true;
    }

    public boolean mouseDragged(double mouseX) {
        if (!dragging) return false;
        setFromMouse(mouseX);
        return true;
    }

    public void mouseReleased() {
        dragging = false;
    }

    /** Arrow keys move it by a hundredth of the range, a tenth with Shift. */
    public boolean keyPressed(int keyCode, boolean shift) {
        if (!usable()) return false;
        int step = Math.max(1, (max - min) / (shift ? 10 : 100));
        if (keyCode == 263) { // left
            setValue(value - step);
            return true;
        }
        if (keyCode == 262) { // right
            setValue(value + step);
            return true;
        }
        return false;
    }

    private void setFromMouse(double mouseX) {
        float t = width <= 0 ? 0 : (float) Mth.clamp((mouseX - x) / width, 0, 1);
        value = min + Math.round(t * (max - min));
    }
}
