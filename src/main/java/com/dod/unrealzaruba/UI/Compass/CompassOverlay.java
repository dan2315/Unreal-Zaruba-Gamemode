package com.dod.unrealzaruba.UI.Compass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.ScoreOverlay;
import com.dod.unrealzaruba.UI.UiFont;
import com.dod.unrealzaruba.UI.ZarubaOverlay;
import com.dod.unrealzaruba.UI.Objectives.HudCapturePointObjective;
import com.dod.unrealzaruba.UI.Objectives.HudDestroyObjective;
import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UI.Respawn.RespawnClientState;
import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsView;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    /** Height of the panel: marker circles with their distances under them, the scale ticks and its numbers. */
    public static final int PANEL_H = 25;
    /** Space between the stacked top-centre panels (score, compass, objectives). */
    public static final int GAP = 3;
    private static final int MARKER_R = 5;
    /** The label that stands for a base: drawn as a little house (not text), a letter would be taken for an objective. */
    private static final String HOME = "#base";
    /** Small text scale for degree numbers and distances. */
    private static final float SMALL = 0.75f;
    /** Markers fade out inside this distance and hide completely up close. */
    private static final double FADE_DIST = 15.0;

    // Brandbook colors (tokens/colors.json): text/text-dim/text-mute, mint accent.
    private static final int MINT = 0xFF00FFA3;
    private static final int TEXT = 0xFFF2F5F3; // text
    private static final int DIM = 0xFFC9CDD2; // text-dim, a little lighter than the brand value for the small numbers
    private static final int MUTE = 0xFF9AA0A8; // text-mute, lifted likewise
    private static final int TICK_MINOR = 0xFF5B5864;
    private static final int TICK_MAJOR = 0xFFB0B5BD;
    private static final int ON_TILE = 0xFF0B0E12;

    private enum Kind { CAPTURE, DESTROY, SUPPLY, RESPAWN, RESPAWN_SELECTED, ENEMY_BASE, PING }

    private record Marker(Vec3 pos, String label, int color, Kind kind, boolean capturing, double dist) {}

    /** True when the compass should be drawn (the objectives under it move with it). */
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

    /** Top of the compass panel: it continues the score panel, or hangs from the top edge without it. */
    private static int top(Font font) {
        return ScoreOverlay.INSTANCE.isShown() ? ScoreOverlay.INSTANCE.bottom(font) : 0; // under the score, or at the top edge without it
    }

    /** Where the compass ends (or where it would start while hidden), for what is drawn under it. */
    public static int bottom(Font font, Minecraft mc, int screenWidth) {
        if (!shouldRender(mc)) return ScoreOverlay.INSTANCE.bottom(font);
        return top(font) + PANEL_H;
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

        int w = HudStyle.topWidth(screenWidth);
        Font font = mc.font; // the game's own font: small text stays crisp
        int x0 = (screenWidth - w) / 2;
        int y0 = top(UiFont.get()); // the score panel above is measured with the brand font
        double centerX = x0 + w / 2.0;
        double halfW = w / 2.0 - 10.0;
        double halfFov = CompassMath.HALF_FOV;
        int midY = y0 + 10; // the scale runs here, and the markers sit on it, their distances under them

        HudStyle.dockedPanel(g, x0, y0, w, PANEL_H, true);
        if (ScoreOverlay.INSTANCE.isShown()) HudStyle.separator(g, x0, y0, w); // the seam with the score above

        List<Marker> markers = collect(pp);
        // Far ones first, so the near one is on top where two overlap
        markers.sort(Comparator.comparingDouble((Marker m) -> m.dist()).reversed());

        List<Double> markerXs = new ArrayList<>(markers.size());
        for (Marker m : markers) {
            double tb = CompassMath.bearingTo(pp.x, pp.z, m.pos().x, m.pos().z);
            double rel = CompassMath.relative(tb, bearing);
            markerXs.add(Math.abs(rel) <= halfFov ? centerX + rel / halfFov * halfW : null);
        }

        g.enableScissor(x0 + 2, y0 + 1, x0 + w - 2, y0 + PANEL_H - 1);

        // The scale: a line with ticks across it, the sides of the world written on it
        g.fill(x0 + 4, midY, x0 + w - 4, midY + 1, TICK_MINOR);
        double min = bearing - halfFov;
        double max = bearing + halfFov;
        for (double b = Math.ceil(min / 5.0) * 5.0; b <= max + 0.001; b += 5.0) {
            double n = CompassMath.normalize360(b);
            double x = centerX + CompassMath.relative(n, bearing) / halfFov * halfW;
            long r = Math.round(n);
            boolean cardinal = r % 90 == 0;
            boolean inter = !cardinal && r % 45 == 0;
            boolean numbered = r % 15 == 0;
            int reach = cardinal || inter ? 0 : numbered ? 3 : 2;
            int tickColor = numbered || cardinal || inter ? TICK_MAJOR : TICK_MINOR;
            int tx = (int) Math.round(x);
            if (cardinal || inter) {
                String label = CompassMath.labelFor(n);
                if (label == null) continue;
                // The sides of the world replace the tick, on a patch of the panel colour so the line does not cross them
                Component text = cardinal ? Component.literal(label).withStyle(ChatFormatting.BOLD) : Component.literal(label);
                int tw = font.width(text);
                // A marker always wins: a side of the world that a marker would touch is left out, the line stays
                boolean taken = false;
                for (Double mx : markerXs) {
                    if (mx != null && Math.abs(mx - x) < MARKER_R + 4 + tw / 2.0) {
                        taken = true;
                        break;
                    }
                }
                if (taken) continue;
                g.fill(tx - tw / 2 - 2, midY - 5, tx + (tw + 1) / 2 + 2, midY + 6, HudStyle.PANEL);
                g.drawString(font, text, tx - tw / 2, midY - 3, cardinal ? TEXT : MINT, true);
            } else {
                g.fill(tx, midY - reach, tx + 1, midY + reach + 1, tickColor);
            }
        }

        // The markers: a circle in the colour of the point on the scale, the distance under it
        for (int i = 0; i < markers.size(); i++) {
            Marker m = markers.get(i);
            Double mx = markerXs.get(i);
            if (mx == null) continue;
            int cx = (int) Math.round(mx);
            int color = fade(m.color(), m.dist());
            if ((color >>> 24) <= 4) continue; // standing on the marker: hide it entirely
            int cy = midY + 0;
            if (m.kind() == Kind.PING) {
                // Pings are diamonds, everything else is a circle: the two never mix up at a glance
                diamond(g, cx, cy, MARKER_R, color);
            } else {
                int rim = m.capturing() && (System.currentTimeMillis() / 400) % 2 == 0 ? fade(TEXT, m.dist())
                        : m.kind() == Kind.RESPAWN_SELECTED ? fade(TEXT, m.dist()) : fade(0xFF000000, m.dist());
                disc(g, cx, cy, MARKER_R + 1, rim);
                disc(g, cx, cy, MARKER_R, color);
            }
            if (HOME.equals(m.label())) {
                house(g, cx, cy, fade(ON_TILE, m.dist()), color);
            } else {
                Component letter = Component.literal(m.label()).withStyle(ChatFormatting.BOLD);
                g.drawString(font, letter, cx - font.width(letter) / 2, cy - 3, fade(ON_TILE, m.dist()), false);
            }

            // The distance is always drawn, even where markers overlap
            String d = Math.round(m.dist()) + "m";
            int dc = fade(DIM, m.dist());
            if ((dc >>> 24) > 4) {
                scaledText(g, font, Component.literal(d), cx - Math.round(font.width(d) * SMALL) / 2, cy + MARKER_R + 3, dc, SMALL);
            }
        }
        g.disableScissor();

        // Centre: the bearing in a small chip
        int ccx = (int) Math.round(centerX);
        String card = CompassMath.cardinalFor(bearing);
        long deg = Math.round(CompassMath.normalize360(bearing)) % 360;
        Component chip = Component.literal(card).withStyle(ChatFormatting.BOLD).withStyle(s -> s.withColor(0x00FFA3))
                .append(Component.literal(" " + deg + "°").withStyle(s -> s.withColor(0xF2F5F3)));
        int cw = font.width(chip);
        int bx = ccx - cw / 2;
        // On the seam with the score above (or at the top edge without it), over a patch of the panel colour
        int chipTop = ScoreOverlay.INSTANCE.isShown() ? y0 - 5 : y0;
        g.fill(bx - 3, chipTop, bx + cw + 3, chipTop + 10, HudStyle.PANEL);
        g.renderOutline(bx - 3, chipTop, cw + 6, 10, HudStyle.SEPARATOR);
        g.drawString(font, chip, bx, chipTop + 1, 0xFFFFFFFF, false);
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
                out.add(new Marker(Vec3.atCenterOf(p.GetPosition()), letter, color, Kind.CAPTURE, capturing, dist(pp, Vec3.atCenterOf(p.GetPosition()))));
            } else if (o instanceof HudDestroyObjective t) {
                out.add(new Marker(Vec3.atCenterOf(t.GetPosition()), "!", HudStyle.ACCENT, Kind.DESTROY, false, dist(pp, Vec3.atCenterOf(t.GetPosition()))));
            }
        }

        BlockPos drop = RespawnClientState.supplyDrop();
        if (drop != null) {
            out.add(new Marker(drop.getCenter(), "$", HudStyle.YELLOW, Kind.SUPPLY, false, dist(pp, drop.getCenter())));
        }

        RespawnPointsView view = RespawnClientState.view();
        if (view != null) {
            Byte effective = view.effectiveId();
            for (RespawnPointsView.Entry e : view.points()) {
                // Capture-point respawns sit on the objective itself; the objective diamond already points there.
                if (e.kind() == RespawnPointsView.Kind.CAPTURE_POINT) continue;
                boolean selected = effective != null && e.id() == effective;
                int color = selected ? 0xFFE8B83A : 0xFF4CC24C; // warn gold / ok green
                out.add(new Marker(e.position().getCenter(), kindInitial(e.kind()), color,
                        selected ? Kind.RESPAWN_SELECTED : Kind.RESPAWN, false, dist(pp, e.position().getCenter())));
            }
            for (RespawnPointsView.EnemyBase b : view.enemyBases()) {
                out.add(new Marker(b.position().getCenter(), HOME, HudStyle.teamColor(b.team()), Kind.ENEMY_BASE, false, dist(pp, b.position().getCenter())));
            }
        }
        // Teammate pings: diamonds in the ping colour. The dead see none (the server only sends to the living).
        if (!RespawnClientState.isDead()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            for (com.dod.unrealzaruba.Radiopings.PingClientState.Entry e : com.dod.unrealzaruba.Radiopings.PingClientState.entries()) {
                Vec3 shown = com.dod.unrealzaruba.Radiopings.PingClientState.liveAnchor(mc == null ? null : mc.level, e);
                out.add(new Marker(shown, e.type().letter, e.type().color, Kind.PING, false, dist(pp, shown)));
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
        demo.add(demoMarker(new BlockPos(ax + 150, ay, az + 40), HOME, HudStyle.RED, Kind.ENEMY_BASE, false, pp));
        return demo;
    }

    private static Marker demoMarker(BlockPos pos, String label, int color, Kind kind, boolean capturing, Vec3 pp) {
        return new Marker(Vec3.atCenterOf(pos), label, color, kind, capturing, dist(pp, Vec3.atCenterOf(pos)));
    }

    private static double dist(Vec3 pp, Vec3 pos) {
        double dx = pos.x - pp.x;
        double dy = pos.y - pp.y;
        double dz = pos.z - pp.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static String kindInitial(RespawnPointsView.Kind kind) {
        return switch (kind) {
            case BASE -> HOME;
            case TENT -> "T";
            case VEHICLE -> "V";
            case CAPTURE_POINT -> "C";
        };
    }

    /** A little house of pixels: a roof, walls and a door that shows the colour of the circle. */
    private static void house(GuiGraphics g, int cx, int cy, int ink, int door) {
        g.fill(cx, cy - 3, cx + 1, cy - 2, ink);
        g.fill(cx - 1, cy - 2, cx + 2, cy - 1, ink);
        g.fill(cx - 2, cy - 1, cx + 3, cy, ink);
        g.fill(cx - 2, cy, cx + 3, cy + 3, ink);
        g.fill(cx, cy + 1, cx + 1, cy + 3, door);
    }

    /** A filled circle, a pixel row at a time. */
    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt((r + 0.5) * (r + 0.5) - dy * dy));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    /** Scaled text with a shadow, so it reads over the panel and the world alike. */
    private static void scaledText(GuiGraphics g, Font font, Component text, int x, int y, int color, float scale) {
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
