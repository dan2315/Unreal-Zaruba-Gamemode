package com.dod.unrealzaruba.Radiopings;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Hand-drawn ping pictograms. Everything is plain {@code GuiGraphics.fill} rectangles,
 * so the icons stay crisp at any size and need no textures. Callers pass an already
 * alpha-adjusted colour; two-tone glyphs derive the dark part from it.
 *
 * Each glyph is centred on ({@code cx}, {@code cy}) and roughly fits a {@code 2*r+1} box.
 */
public enum PingGlyph {
    EXCLAMATION,
    WARN_TRIANGLE,
    DOWN_ARROW,
    CIRCULAR_ARROW,
    PALM,
    OK_HAND,
    THUMBS_UP,
    DOUBLE_CHEVRON,
    SHIELD;

    public void draw(GuiGraphics g, int cx, int cy, int r, int color) {
        switch (this) {
            case EXCLAMATION -> exclamation(g, cx, cy, r, color);
            case WARN_TRIANGLE -> warnTriangle(g, cx, cy, r, color);
            case DOWN_ARROW -> downArrow(g, cx, cy, r, color);
            case CIRCULAR_ARROW -> circularArrow(g, cx, cy, r, color);
            case PALM -> palm(g, cx, cy, r, color);
            case OK_HAND -> okHand(g, cx, cy, r, color);
            case THUMBS_UP -> thumbsUp(g, cx, cy, r, color);
            case DOUBLE_CHEVRON -> doubleChevron(g, cx, cy, r, color);
            case SHIELD -> shield(g, cx, cy, r, color);
        }
    }

    private static int darken(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.round(((argb >> 16) & 0xFF) * factor);
        int gg = Math.round(((argb >> 8) & 0xFF) * factor);
        int b = Math.round((argb & 0xFF) * factor);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    /** Bold red "!": vertical bar plus a dot. */
    private static void exclamation(GuiGraphics g, int cx, int cy, int r, int color) {
        int barW = Math.max(2, Math.round(r * 0.55f));
        int barTop = cy - r;
        int barH = Math.round((2 * r + 1) * 0.62f);
        int dot = Math.max(2, barW + 1);
        g.fill(cx - barW / 2, barTop, cx + (barW + 1) / 2, barTop + barH, color);
        g.fill(cx - dot / 2, cy + r - dot + 1, cx + (dot + 1) / 2, cy + r + 1, color);
    }

    /** Yellow warning triangle with a dark "!" on it. */
    private static void warnTriangle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int row = 0; row <= 2 * r; row++) {
            int half = (int) ((double) row / (2 * r) * r);
            int y = cy - r + row;
            g.fill(cx - half, y, cx + half + 1, y + 1, color);
        }
        int dark = darken(color, 0.25f);
        int barW = Math.max(2, r / 3);
        int barTop = cy - r / 2;
        int barH = Math.max(2, r);
        g.fill(cx - barW / 2, barTop, cx + (barW + 1) / 2, barTop + barH, dark);
        int dot = barW + 1;
        g.fill(cx - dot / 2, barTop + barH + 1, cx + (dot + 1) / 2, barTop + barH + 1 + dot, dark);
    }

    /** Bold down arrow: shaft plus a downward head. */
    private static void downArrow(GuiGraphics g, int cx, int cy, int r, int color) {
        int shaftW = Math.max(2, (2 * r + 1) / 4);
        int shaftTop = cy - r;
        int shaftBottom = cy + r / 3;
        g.fill(cx - shaftW / 2, shaftTop, cx + (shaftW + 1) / 2, shaftBottom, color);
        int headH = cy + r - shaftBottom + 1;
        for (int row = 0; row <= headH; row++) {
            int half = (int) ((double) (headH - row) / headH * r);
            int y = shaftBottom + row;
            g.fill(cx - half, y, cx + half + 1, y + 1, color);
        }
    }

    /** Circular arrow: an almost-closed ring plus a head on the end. */
    private static void circularArrow(GuiGraphics g, int cx, int cy, int r, int color) {
        double gapHalf = Math.toRadians(35.0);
        double a = Math.toRadians(90.0) + gapHalf;
        double end = Math.toRadians(90.0) + Math.PI * 2.0 - gapHalf;
        double step = Math.max(0.08, 1.5 / Math.max(1.0, r));
        for (; a < end; a += step) {
            int x = cx + (int) Math.round(Math.cos(a) * r);
            int y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
        // Head at the gap, pointing along the travel direction.
        double ha = Math.toRadians(90.0) + gapHalf;
        int hx = cx + (int) Math.round(Math.cos(ha) * r);
        int hy = cy + (int) Math.round(Math.sin(ha) * r);
        double tx = -Math.sin(ha);
        double ty = Math.cos(ha);
        double px = -ty;
        double py = tx;
        g.fill(hx - 1, hy - 1, hx + 1, hy + 1, color);
        for (int k = 1; k <= 2; k++) {
            int bx = (int) Math.round(hx - tx * k * 2.2);
            int by = (int) Math.round(hy - ty * k * 2.2);
            int spread = k + 1;
            int lx = (int) Math.round(bx + px * spread);
            int ly = (int) Math.round(by + py * spread);
            int rx = (int) Math.round(bx - px * spread);
            int ry = (int) Math.round(by - py * spread);
            g.fill(lx - 1, ly - 1, lx + 1, ly + 1, color);
            g.fill(rx - 1, ry - 1, rx + 1, ry + 1, color);
        }
    }

    /** Simplified open palm: four fingers, a thumb and the palm block. */
    private static void palm(GuiGraphics g, int cx, int cy, int r, int color) {
        int palmW = Math.max(3, r);
        int palmTop = cy - r / 3;
        int palmBottom = cy + r;
        g.fill(cx - palmW / 2, palmTop, cx + (palmW + 1) / 2, palmBottom + 1, color);
        int fingerW = Math.max(1, palmW / 4);
        for (int f = 0; f < 4; f++) {
            int fx = cx - palmW / 2 + f * (palmW / 4);
            int len = r + (f == 1 || f == 2 ? 1 : 0);
            g.fill(fx, palmTop - len, fx + fingerW, palmTop + 1, color);
        }
        g.fill(cx - palmW / 2 - 2, cy, cx - palmW / 2, cy + r / 2, color);
    }

    /** OK hand: ring plus wrist. */
    private static void okHand(GuiGraphics g, int cx, int cy, int r, int color) {
        int rad = Math.max(2, r * 2 / 3);
        int oy = cy - r / 4;
        for (int dy = -rad; dy <= rad; dy++) {
            int half = (int) Math.round(Math.sqrt((double) (rad + 0.5) * (rad + 0.5) - dy * dy));
            int inner = Math.max(0, half - 2);
            if (half <= 0) continue;
            int y = oy + dy;
            g.fill(cx - half, y, cx - inner, y + 1, color);
            g.fill(cx + inner + 1, y, cx + half + 1, y + 1, color);
        }
        int wristW = Math.max(2, rad / 2);
        g.fill(cx - wristW / 2, oy + rad, cx + (wristW + 1) / 2, cy + r + 1, color);
    }

    /** Thumbs up: fist block with the thumb sticking up on the left. */
    private static void thumbsUp(GuiGraphics g, int cx, int cy, int r, int color) {
        int fistW = Math.max(4, r);
        int fistTop = cy - r / 3;
        int fistBottom = cy + r;
        g.fill(cx - fistW / 2 + 1, fistTop, cx + (fistW + 1) / 2 + 1, fistBottom + 1, color);
        int thumbW = Math.max(2, r / 3);
        g.fill(cx - fistW / 2 - 1, cy - r, cx - fistW / 2 - 1 + thumbW, fistTop + 2, color);
    }

    /** Double chevron (>>) for backup. */
    private static void doubleChevron(GuiGraphics g, int cx, int cy, int r, int color) {
        int h = Math.max(3, r);
        for (int c = 0; c < 2; c++) {
            int ox = cx - h / 2 + c * (h / 2 + 1);
            for (int row = 0; row <= h; row++) {
                int off = Math.abs(h / 2 - row);
                int y = cy - h / 2 + row;
                g.fill(ox - off, y, ox - off + 2, y + 1, color);
            }
        }
    }

    /** Shield: block on top, point at the bottom. */
    private static void shield(GuiGraphics g, int cx, int cy, int r, int color) {
        int top = cy - r;
        int mid = cy + r / 4;
        g.fill(cx - (r * 2 / 3), top, cx + (r * 2 / 3) + 1, mid + 1, color);
        int h = cy + r - mid;
        for (int row = 0; row <= h; row++) {
            int half = (int) ((double) (h - row) / h * (r * 2 / 3));
            int y = mid + row;
            g.fill(cx - half, y, cx + half + 1, y + 1, color);
        }
    }

    /** A filled circle, a pixel row at a time. */
    public static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt((r + 0.5) * (r + 0.5) - dy * dy));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    /** A filled diamond, a pixel row at a time. */
    public static void diamond(GuiGraphics g, int cx, int cy, int hw, int hh, int color) {
        for (int dy = -hh; dy <= hh; dy++) {
            int half = (int) ((1.0 - Math.abs(dy) / (double) hh) * hw);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }
}
