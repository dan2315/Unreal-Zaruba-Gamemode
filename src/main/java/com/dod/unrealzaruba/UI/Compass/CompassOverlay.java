package com.dod.unrealzaruba.UI.Compass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.ZarubaOverlay;
import com.dod.unrealzaruba.UI.Objectives.HudCapturePointObjective;
import com.dod.unrealzaruba.UI.Objectives.HudDestroyObjective;
import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UI.Respawn.RespawnClientState;
import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Battlefield-style compass strip at the top center of the screen.
 *
 * <ul>
 *   <li>Ticks every 5 degrees, numbers every 15 degrees, N/NE/E/SE/S/SW/W/NW letters.</li>
 *   <li>Center caret + current bearing ("S · 182°") below the strip.</li>
 *   <li>Markers from the same data Xaero waypoints use: capture points (letter, owner color,
 *   capturing team color while being captured), destroy targets, supply drop, the player's own
 *   respawn points (per-team, selected one gold) and enemy bases. Per-team because
 *   {@link RespawnClientState#view()} is per-player.</li>
 *   <li>Transparent floating strip (no panel): brandbook text/text-dim/text-mute
 *   and mint accent (tokens/colors.json). Team/warning colors keep their gameplay
 *   meaning (same values as {@link HudStyle} so the strip matches the objectives panel).</li>
 * </ul>
 */
public class CompassOverlay implements ZarubaOverlay {
    public static final CompassOverlay INSTANCE = new CompassOverlay();

    /**
     * Debug bypass: when true, the strip renders anywhere (any dimension, no objectives)
     * with demo markers, so singleplayer testing is just {@code /compass}.
     * Toggled by the {@code /compass} command. Volatile: in singleplayer the integrated
     * server (which runs the command) and the client (which renders) share the JVM.
     */
    public static volatile boolean DEBUG_FORCE = false;
    /** World anchor for demo markers, captured once (see collect). */
    private static Vec3 debugAnchor = null;

    /** Flips {@link #DEBUG_FORCE}, returns the new state. */
    public static boolean toggleDebug() {
        DEBUG_FORCE = !DEBUG_FORCE;
        if (!DEBUG_FORCE) debugAnchor = null; // re-anchor around the player on next ON
        return DEBUG_FORCE;
    }

    private static final ResourceLocation GAME_DIM =
            ResourceLocation.fromNamespaceAndPath("unrealzaruba", "game_dim");

    public static final int MAX_WIDTH = 240;
    public static final int MIN_WIDTH = 160;
    /** Height of the transparent strip (marker letters + icons + degree row). */
    public static final int STRIP_H = 18;
    private static final int INFO_H = 9;
    private static final int GAP = 2;
    /** Full height incl. the bearing line below the strip. ScoreOverlay uses this to move down. */
    public static final int TOTAL_H = STRIP_H + GAP + INFO_H;
    private static final int TOP_Y = 4;
    /** Small text scale for degree numbers and marker letters, so rows don't collide. */
    private static final float SMALL = 0.7f;
    /** Markers fade out inside this distance and hide completely up close. */
    private static final double FADE_DIST = 15.0;

    // Transparent floating strip: no background panel, only text/markers/ticks.
    // Brandbook colors (tokens/colors.json): text/text-dim/text-mute, mint accent.
    private static final int MINT = 0xFF00FFA3;
    private static final int TEXT = 0xFFF2F5F3; // text
    private static final int DIM = 0xFFB9BDC2; // text-dim
    private static final int MUTE = 0xFF7C828A; // text-mute
    private static final int TICK_MINOR = 0xFF4A4753;
    private static final int TICK_MAJOR = 0xFF8E939B;
    private static final int CENTER_LINE = 0x8000FFA3; // mint, 50%

    private enum Kind { CAPTURE, DESTROY, SUPPLY, RESPAWN, RESPAWN_SELECTED, ENEMY_BASE }

    private record Marker(BlockPos pos, String label, int color, Kind kind, boolean capturing, double dist) {}

    /** True when the strip should be drawn (also used by ScoreOverlay to shift down). */
    public static boolean shouldRender(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) return false;
        if (mc.screen != null) return false;
        if (DEBUG_FORCE) return true; // /compass: bypass dimension + objectives checks
        if (mc.level.dimension() == null || !GAME_DIM.equals(mc.level.dimension().location())) return false;
        return hasAnything();
    }

    private static boolean hasAnything() {
        for (HudObjective o : ObjectivesOverlay.INSTANCE.objectives()) {
            if (o instanceof HudCapturePointObjective || o instanceof HudDestroyObjective) return true;
        }
        if (RespawnClientState.supplyDrop() != null) return true;
        RespawnPointsView v = RespawnClientState.view();
        return v != null && (!v.points().isEmpty() || !v.enemyBases().isEmpty());
    }

    /** Offset for panels below the compass (strip + gap + margin). */
    public static int topOffset() {
        return TOP_Y + TOTAL_H + HudStyle.MARGIN;
    }

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (!shouldRender(mc)) return;

        float yaw = Mth.lerp(partialTick, mc.player.yRotO, mc.player.getYRot());
        double bearing = CompassMath.yawToBearing(yaw);
        // Interpolated position (not raw tick position): otherwise markers jump in 20Hz steps
        // while the yaw-interpolated ticks glide smoothly, and markers shake against the scale.
        Vec3 pp = mc.player.getPosition(partialTick);

        int w = Math.min(MAX_WIDTH, screenWidth - 24);
        if (w < MIN_WIDTH) return;
        int x0 = (screenWidth - w) / 2;
        int y0 = TOP_Y;
        double centerX = x0 + w / 2.0;
        double halfW = w / 2.0 - 6.0;
        double halfFov = CompassMath.HALF_FOV;

        List<Marker> markers = collect(pp);
        markers.sort(Comparator.comparingDouble(m ->
                CompassMath.relative(CompassMath.bearingTo(pp.x, pp.z, m.pos().getCenter().x, m.pos().getCenter().z), bearing)));

        // Transparent: no panel fill, scissor only clips edge ticks/markers.

        // Marker screen positions (null = off-screen, handled as edge arrows).
        List<Double> markerXs = new ArrayList<>(markers.size());
        for (Marker m : markers) {
            double tb = CompassMath.bearingTo(pp.x, pp.z, m.pos().getCenter().x, m.pos().getCenter().z);
            double rel = CompassMath.relative(tb, bearing);
            markerXs.add(Math.abs(rel) <= halfFov ? centerX + rel / halfFov * halfW : null);
        }

        Font font = gui.getFont();
        g.enableScissor(x0 + 1, y0 + 1, x0 + w - 1, y0 + STRIP_H - 1);

        // Ticks + degree labels along the bottom; marker icons live in the top rows,
        // so labels never sit on top of markers.
        double min = bearing - halfFov;
        double max = bearing + halfFov;
        for (double b = Math.ceil(min / 5.0) * 5.0; b <= max + 0.001; b += 5.0) {
            double n = CompassMath.normalize360(b);
            double x = centerX + CompassMath.relative(n, bearing) / halfFov * halfW;
            long r = Math.round(n);
            boolean cardinal = r % 90 == 0;
            boolean inter = !cardinal && r % 45 == 0;
            boolean numbered = r % 15 == 0;
            int tickTop;
            int tickColor;
            int tickBottom = y0 + STRIP_H - 3;
            if (cardinal || inter) {
                tickTop = y0 + STRIP_H - 8;
                tickColor = TICK_MAJOR;
            } else if (numbered) {
                tickTop = y0 + STRIP_H - 6;
                tickColor = TICK_MAJOR;
            } else {
                tickTop = y0 + STRIP_H - 5;
                tickColor = TICK_MINOR;
            }
            g.fill((int) Math.round(x), tickTop, (int) Math.round(x) + 1, tickBottom, tickColor);

            if (numbered) {
                String label = CompassMath.labelFor(n);
                if (label == null) continue;
                boolean nearMarker = false;
                for (Double mx : markerXs) {
                    if (mx != null && Math.abs(mx - x) < 8) {
                        nearMarker = true;
                        break;
                    }
                }
                if (nearMarker) continue;
                int color = cardinal ? TEXT : (inter ? MINT : DIM);
                int lx = (int) Math.round(x) - HudStyle.width(font, label, SMALL) / 2;
                smallText(g, font, label, lx, y0 + 11, color, SMALL);
            }
        }

        // Markers: letter row on top, icon below it, distance at the bottom.
        // Colliding neighbours alternate into a lower slot; their distance is skipped.
        double lastX = Double.NaN;
        boolean alt = false;
        for (int i = 0; i < markers.size(); i++) {
            Marker m = markers.get(i);
            Double mx = markerXs.get(i);
            if (mx == null) continue;
            if (!Double.isNaN(lastX) && Math.abs(mx - lastX) < 13) {
                alt = !alt;
            } else {
                alt = false;
            }
            lastX = mx;
            int cx = (int) Math.round(mx);
            int letterY = y0 + (alt ? 5 : 0);
            int iconY = y0 + (alt ? 13 : 9);
            int color = m.color();
            if (m.capturing() && (System.currentTimeMillis() / 500) % 2 == 0) {
                color = TEXT; // blink white while being captured
            }
            color = fade(color, m.dist());
            if ((color >>> 24) <= 4) continue; // standing on the marker: hide it entirely
            int labelW = HudStyle.width(font, m.label(), SMALL);
            switch (m.kind()) {
                case CAPTURE, DESTROY, SUPPLY, ENEMY_BASE -> {
                    diamond(g, cx, iconY, 2, color);
                    smallText(g, font, m.label(), cx - labelW / 2, letterY, color, SMALL);
                }
                case RESPAWN -> {
                    triangleUp(g, cx, iconY, 2, color);
                    smallText(g, font, m.label(), cx - labelW / 2, letterY, color, SMALL);
                }
                case RESPAWN_SELECTED -> {
                    diamond(g, cx, iconY, 3, fade(0x40E8B83A, m.dist())); // soft gold halo for the chosen point
                    diamond(g, cx, iconY, 2, color);
                    smallText(g, font, m.label(), cx - labelW / 2, letterY, color, SMALL);
                }
            }
            if (!alt) {
                String d = Math.round(m.dist()) + "m";
                int dc = fade(MUTE, m.dist());
                if ((dc >>> 24) > 4) {
                    smallText(g, font, d, cx - HudStyle.width(font, d, SMALL) / 2, y0 + 11, dc, SMALL);
                }
            }
        }

        // Center caret: mint wedge + translucent vertical.
        g.fill((int) Math.round(centerX), y0 + 5, (int) Math.round(centerX) + 1, y0 + STRIP_H - 1, CENTER_LINE);
        g.disableScissor();

        int ccx = (int) Math.round(centerX);
        g.fill(ccx - 4, y0 + 1, ccx + 5, y0 + 2, MINT);
        g.fill(ccx - 3, y0 + 2, ccx + 4, y0 + 3, MINT);
        g.fill(ccx - 1, y0 + 3, ccx + 2, y0 + 5, MINT);

        // Current bearing below the strip: cardinal in mint, degrees in main text.
        float infoScale = 0.8f;
        String card = CompassMath.cardinalFor(bearing);
        long deg = Math.round(CompassMath.normalize360(bearing)) % 360;
        String degText = deg + "°";
        String sep = " · ";
        int cardW = HudStyle.width(font, card, infoScale);
        int sepW = HudStyle.width(font, sep, infoScale);
        int sx = ccx - (cardW + sepW + HudStyle.width(font, degText, infoScale)) / 2;
        int sy = y0 + STRIP_H + GAP;
        smallText(g, font, card, sx, sy, MINT, infoScale);
        smallText(g, font, sep, sx + cardW, sy, MUTE, infoScale);
        smallText(g, font, degText, sx + cardW + sepW, sy, TEXT, infoScale);
    }

    private static List<Marker> collect(Vec3 pp) {
        List<Marker> out = new ArrayList<>();

        for (HudObjective o : ObjectivesOverlay.INSTANCE.objectives()) {
            if (o instanceof HudCapturePointObjective p) {
                String letter = ObjectivesOverlay.INSTANCE.letterFor(p.GetRuntimeId());
                if (letter == null) letter = "?";
                boolean capturing = p.getCapturedBy() != null && p.getCapturedBy() != p.getOwner() && p.getProgress() > 0;
                boolean held = p.getOwner() != null && !capturing;
                int color;
                if (capturing) {
                    color = HudStyle.teamColor(p.getCapturedBy());
                } else if (held) {
                    color = HudStyle.teamColor(p.getOwner());
                } else {
                    color = HudStyle.NEUTRAL;
                }
                out.add(new Marker(p.GetPosition(), letter, color, Kind.CAPTURE, capturing, dist(pp, p.GetPosition())));
            } else if (o instanceof HudDestroyObjective t) {
                out.add(new Marker(t.GetPosition(), "!", HudStyle.ACCENT, Kind.DESTROY, false, dist(pp, t.GetPosition())));
            }
        }

        BlockPos drop = RespawnClientState.supplyDrop();
        if (drop != null) {
            out.add(new Marker(drop, "$", HudStyle.YELLOW, Kind.SUPPLY, false, dist(pp, drop)));
        }

        RespawnPointsView view = RespawnClientState.view();
        if (view != null) {
            Byte effective = view.effectiveId();
            for (RespawnPointsView.Entry e : view.points()) {
                // Capture-point respawns sit on the objective itself; the objective diamond already points there.
                if (e.kind() == RespawnPointsView.Kind.CAPTURE_POINT) continue;
                boolean selected = effective != null && e.id() == effective;
                int color = selected ? 0xFFE8B83A : 0xFF4CC24C; // warn gold / ok green
                out.add(new Marker(e.position(), kindInitial(e.kind()), color,
                        selected ? Kind.RESPAWN_SELECTED : Kind.RESPAWN, false, dist(pp, e.position())));
            }
            for (RespawnPointsView.EnemyBase b : view.enemyBases()) {
                out.add(new Marker(b.position(), "E", HudStyle.teamColor(b.team()), Kind.ENEMY_BASE, false, dist(pp, b.position())));
            }
        }
        if (DEBUG_FORCE && out.isEmpty()) {
            // Anchor demo markers once around the player: rebuilding them from the live
            // position every frame snaps every marker by a block at integer coords.
            if (debugAnchor == null || debugAnchor.distanceToSqr(pp) > 160000.0) {
                debugAnchor = new Vec3(Math.floor(pp.x) + 0.5, pp.y, Math.floor(pp.z) + 0.5);
            }
            out.addAll(demoMarkers(debugAnchor, pp));
        }
        return out;
    }

    /**
     * Demo markers around the player for {@code /compass} testing in singleplayer:
     * held red/blue points, a neutral one, a blinking capturing one, destroy/supply,
     * respawns (plain + selected) and a far enemy base to trigger the edge arrow.
     */
    private static List<Marker> demoMarkers(Vec3 anchor, Vec3 pp) {
        int ax = (int) Math.floor(anchor.x);
        int ay = (int) Math.round(anchor.y);
        int az = (int) Math.floor(anchor.z);
        List<Marker> demo = new ArrayList<>();
        demo.add(demoMarker(new BlockPos(ax, ay, az - 40), "A", HudStyle.RED, Kind.CAPTURE, false, pp));
        demo.add(demoMarker(new BlockPos(ax + 60, ay, az), "B", HudStyle.BLUE, Kind.CAPTURE, false, pp));
        demo.add(demoMarker(new BlockPos(ax, ay, az + 50), "C", HudStyle.NEUTRAL, Kind.CAPTURE, false, pp));
        demo.add(demoMarker(new BlockPos(ax - 35, ay, az + 20), "D", HudStyle.BLUE, Kind.CAPTURE, true, pp));
        demo.add(demoMarker(new BlockPos(ax + 70, ay, az - 70), "!", HudStyle.ACCENT, Kind.DESTROY, false, pp));
        demo.add(demoMarker(new BlockPos(ax - 30, ay, az - 5), "$", HudStyle.YELLOW, Kind.SUPPLY, false, pp));
        demo.add(demoMarker(new BlockPos(ax - 15, ay, az + 18), "T", 0xFF4CC24C, Kind.RESPAWN, false, pp));
        demo.add(demoMarker(new BlockPos(ax + 12, ay, az - 22), "B", 0xFFE8B83A, Kind.RESPAWN_SELECTED, false, pp));
        demo.add(demoMarker(new BlockPos(ax + 150, ay, az + 40), "E", HudStyle.RED, Kind.ENEMY_BASE, false, pp));
        return demo;
    }

    private static Marker demoMarker(BlockPos pos, String label, int color, Kind kind, boolean capturing, Vec3 pp) {
        return new Marker(pos, label, color, kind, capturing, dist(pp, pos));
    }

    private static double dist(Vec3 pp, BlockPos pos) {
        Vec3 c = pos.getCenter();
        double dx = c.x - pp.x;
        double dy = c.y - pp.y;
        double dz = c.z - pp.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static String kindInitial(RespawnPointsView.Kind kind) {
        return switch (kind) {
            case BASE -> "B";
            case TENT -> "T";
            case VEHICLE -> "V";
            case CAPTURE_POINT -> "C";
        };
    }

    private static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    /** Scaled text with shadow (readable over bright sky on the transparent strip). */
    private static void smallText(GuiGraphics g, Font font, String text, int x, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, true);
        g.pose().popPose();
    }

    /** Linear fade inside {@link #FADE_DIST} blocks; 0 alpha up close. */
    private static int fade(int argb, double dist) {
        if (dist >= FADE_DIST) return argb;
        int alpha = (int) Math.round((argb >>> 24) * Math.max(0, dist / FADE_DIST));
        if (alpha <= 0) return 0;
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    private static void triangleUp(GuiGraphics g, int cx, int cy, int r, int color) {
        // Isosceles triangle pointing up: tip at cy - r, base (2*r wide) at cy + r.
        for (int dy = -r; dy <= r; dy++) {
            int half = (dy + r) / 2;
            int y = cy + dy;
            g.fill(cx - half, y, cx + half + 1, y + 1, color);
        }
    }
}
