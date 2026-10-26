package com.dod.unrealzaruba.UI.Compass;

import java.util.List;

import com.dod.unrealzaruba.Radiopings.PingClientState;
import com.dod.unrealzaruba.Radiopings.RadioDebug;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.UiFont;
import com.dod.unrealzaruba.UI.ZarubaOverlay;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/**
 * Ping markers as dots on the screen, Counter-Strike style: a constant-size dot with the distance over it,
 * readable at any range. Off-screen pings stick to the screen edge as arrows pointing their way.
 * The world-space sonar is gone; this overlay replaces it.
 */
public class PingScreenOverlay implements ZarubaOverlay {
    public static final PingScreenOverlay INSTANCE = new PingScreenOverlay();

    private static final int DOT_R = 4;
    private static final int CENTER_R = 2;
    private static final int RING = 0xFFFFFFFF;
    private static final int BACKING = 0xFF101215;
    private static final int EDGE_MARGIN = 16;
    private static final double NEAR_HIDE_DIST = 2.0;
    private static final double LABEL_MAX_DIST = 40.0;
    private static final long FADE_OUT_MS = 2000;
    private static long lastDebugLogMs;

    /** Where a ping lands on the screen, or which way to look for it. */
    public record Projection(float x, float y, boolean behind, boolean onScreen) {}

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;
        if (com.dod.unrealzaruba.UI.Respawn.RespawnClientState.isDead() || mc.player.isSpectator()) return;
        List<PingClientState.Entry> pings = PingClientState.entries();
        if (pings.isEmpty()) return;
        if (RadioDebug.DEBUG_FORCE) {
            long nowLog = System.currentTimeMillis();
            if (nowLog - lastDebugLogMs > 1000) {
                lastDebugLogMs = nowLog;
                com.dod.unrealzaruba.UnrealZaruba.LOGGER.info("[PingDebug] drawing {} ping(s)", pings.size());
            }
        }
        if (RadioDebug.DEBUG_FORCE) {
            long nowLog = System.currentTimeMillis();
            if (nowLog - lastDebugLogMs > 1000) {
                lastDebugLogMs = nowLog;
                com.dod.unrealzaruba.UnrealZaruba.LOGGER.info("[PingDebug] drawing {} ping(s)", pings.size());
            }
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        Matrix4f view = new Matrix4f().rotation(new Quaternionf(camera.rotation()).conjugate());
        Matrix4f proj = mc.gameRenderer.getProjectionMatrix((double) mc.options.fov().get());
        Font font = UiFont.get();
        long now = System.currentTimeMillis();

        for (PingClientState.Entry ping : pings) {
            long remaining = ping.expireAt() - now;
            if (remaining <= 0) continue;
            float life = Math.min(1f, remaining / (float) FADE_OUT_MS);
            Vec3 shown = PingClientState.liveAnchor(mc.level, ping);
            double dist = Math.sqrt(shown.distanceToSqr(eye));
            if (dist < NEAR_HIDE_DIST) continue; // standing on it: nothing to point at

            Projection p = project(shown.subtract(eye), view, proj, screenWidth, screenHeight);
            String d = Math.round(dist) + "m";
            if (p.onScreen()) {
                int cx = Math.round(p.x());
                int cy = Math.round(p.y());
                int ring = withAlpha(RING, life);
                int fill = withAlpha(ping.type().color, life);
                disc(g, cx, cy, DOT_R, ring);
                disc(g, cx, cy, DOT_R - 1, withAlpha(BACKING, life));
                disc(g, cx, cy, CENTER_R, fill);
                Component distText = Component.literal(d);
                int tw = font.width(distText);
                g.drawString(font, distText, cx - tw / 2, cy - DOT_R - 11, withAlpha(0xFFF2F5F3, life), true);
                if (dist <= LABEL_MAX_DIST) {
                    Component name = Component.literal(ping.senderName());
                    int nw = font.width(name);
                    g.drawString(font, name, cx - nw / 2, cy + DOT_R + 3, withAlpha(HudStyle.MUTED, life), true);
                }
            } else {
                float[] edge = clampToEdge(p.x(), p.y(), screenWidth, screenHeight, EDGE_MARGIN);
                int ex = Math.round(edge[0]);
                int ey = Math.round(edge[1]);
                double angle = Math.atan2(edge[2], -edge[3]);
                int color = withAlpha(ping.type().color, life);
                arrow(g, ex, ey, (float) angle, 7, color);
                Component distText = Component.literal(d);
                int tw = font.width(distText);
                int tx = Math.max(2, Math.min(screenWidth - tw - 2, ex - tw / 2));
                int ty = Math.min(ey + 10, screenHeight - 12);
                g.drawString(font, distText, tx, ty, withAlpha(0xFFF2F5F3, life), true);
            }
        }
    }

    /**
     * Projects a camera-relative position to pixels. Behind the camera the point is mirrored,
     * so the caller can clamp it to the edge as a "turn around" hint.
     */
    public static Projection project(Vec3 rel, Matrix4f view, Matrix4f proj, int w, int h) {
        Vector4f v = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1f);
        view.transform(v);
        if (v.z > -0.001f) {
            // Behind or inside the camera: direction only, from camera-space x/y.
            float dx = v.x;
            float dy = v.y;
            if (Math.abs(dx) < 1e-6f && Math.abs(dy) < 1e-6f) {
                dx = 0f;
                dy = 1f;
            }
            float n = (float) Math.sqrt(dx * dx + dy * dy);
            float sx = w / 2f + dx / n * w;
            float sy = h / 2f - dy / n * h;
            return new Projection(sx, sy, true, false);
        }
        proj.transform(v);
        float inv = 1f / v.w;
        float sx = (v.x * inv * 0.5f + 0.5f) * w;
        float sy = (1f - (v.y * inv * 0.5f + 0.5f)) * h;
        boolean onScreen = sx >= EDGE_MARGIN && sx <= w - EDGE_MARGIN && sy >= EDGE_MARGIN && sy <= h - EDGE_MARGIN;
        return new Projection(sx, sy, false, onScreen);
    }

    /** Clamps a point into the inset rect; returns {x, y, dirX, dirY}: the spot and the outward direction. */
    public static float[] clampToEdge(float x, float y, int w, int h, int margin) {
        float cx = w / 2f;
        float cy = h / 2f;
        float dx = x - cx;
        float dy = y - cy;
        if (Math.abs(dx) < 1e-6f && Math.abs(dy) < 1e-6f) {
            dx = 0f;
            dy = -1f;
        }
        float tx = dx == 0f ? Float.POSITIVE_INFINITY : (cx - margin) / Math.abs(dx);
        float ty = dy == 0f ? Float.POSITIVE_INFINITY : (cy - margin) / Math.abs(dy);
        float t = Math.min(tx, ty);
        return new float[]{cx + dx * t, cy + dy * t, dx, dy};
    }

    private static int withAlpha(int argb, float alpha) {
        int a = Math.round((argb >>> 24) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** A filled circle, a pixel row at a time. */
    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt((r + 0.5) * (r + 0.5) - dy * dy));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    /** A triangle pointing along angle (screen coords, 0 = up), drawn in the rotated pose. */
    private void arrow(GuiGraphics g, int cx, int cy, float angle, int size, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(angle));
        for (int row = 0; row <= size; row++) {
            int half = row / 2;
            int y = -size + row;
            g.fill(-half, y, half + 1, y + 1, color);
        }
        g.pose().popPose();
    }
}
