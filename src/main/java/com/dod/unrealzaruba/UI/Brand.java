package com.dod.unrealzaruba.UI;

import net.minecraft.client.gui.GuiGraphics;

/** Unreal Frontiers brand tokens (tokens/colors.json of the brand book) for screens that follow it. */
public final class Brand {
    public static final int MINT = 0xFF00FFA3;
    public static final int CYAN = 0xFF00E5FF;
    public static final int BLUE = 0xFF2979FF;
    public static final int MINT_DIM = 0xFF00B377;
    public static final int CYAN_DIM = 0xFF00C8E5;
    public static final int BLUE_BRIGHT = 0xFF4D94FF;
    public static final int WINDOW = 0xFF100F12;
    public static final int PANEL = 0xFF1B191F;
    public static final int CARD = 0xFF201E24;
    public static final int CARD_HI = 0xFF2A2731;
    public static final int BORDER = 0xFF3A3842;
    public static final int DIVIDER = 0xFF2E2C34;
    public static final int BTN = 0xFF242228;
    public static final int BTN_HI = 0xFF302D38;
    public static final int BTN_BORDER = 0xFF3B3943;
    public static final int TEXT = 0xFFF2F5F3;
    public static final int TEXT_DIM = 0xFFB9BDC2;
    public static final int TEXT_MUTE = 0xFF7C828A;
    public static final int OK = 0xFF4CC24C;
    public static final int WARN = 0xFFE8B83A;
    public static final int BAR_TRACK = 0xFF26232C;
    public static final int DISABLED = 0xFF3A3842;

    private Brand() {}

    /** The brand gradient, left to right, in three stops. */
    public static void gradient(GuiGraphics g, int x, int y, int w, int h, int a, int b, int c) {
        for (int i = 0; i < w; i++) {
            float t = w <= 1 ? 0 : i / (float) (w - 1);
            int color = t < 0.5f ? mix(a, b, t * 2) : mix(b, c, (t - 0.5f) * 2);
            g.fill(x + i, y, x + i + 1, y + h, color);
        }
    }

    public static int mix(int from, int to, float t) {
        int r = (int) (((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int gr = (int) (((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = (int) ((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    public static int alpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }
}
