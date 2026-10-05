package com.dod.unrealzaruba.UI;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Look of the match HUD (capture points board): dark translucent panels, team colors, orange accents. */
public final class HudStyle {
    public static final int PANEL = 0xC70A0D12;
    public static final int PANEL_BORDER = 0xFF3A434E;
    public static final int DIVIDER = 0xFF2C353F;
    public static final int BAR_BG = 0xFF27303A;
    public static final int TEXT = 0xFFEEF1F4;
    public static final int MUTED = 0xFFC3CAD2;
    public static final int DIM = 0xFFAEB7C1;
    public static final int ACCENT = 0xFFF2A33A;
    public static final int ON_COLOR = 0xFF0B0E12;
    public static final int RED = 0xFFFF5C61;
    public static final int BLUE = 0xFF5AA2FF;
    public static final int YELLOW = 0xFFF2C94C;
    public static final int PURPLE = 0xFFB07CFF;
    public static final int NEUTRAL = 0xFF8D97A3;

    public static final int MARGIN = 8;

    private HudStyle() {}

    public static int teamColor(TeamColor team) {
        if (team == null) return NEUTRAL;
        return switch (team) {
            case RED -> RED;
            case BLUE -> BLUE;
            case YELLOW -> YELLOW;
            case PURPLE -> PURPLE;
            default -> NEUTRAL;
        };
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.renderOutline(x, y, w, h, PANEL_BORDER);
    }

    public static void text(GuiGraphics g, Font font, String text, int x, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    public static void text(GuiGraphics g, Font font, Component text, int x, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    public static void text(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    public static int width(Font font, String text, float scale) {
        return Math.round(font.width(text) * scale);
    }

    public static String trim(Font font, String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
    }
}
