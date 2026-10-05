package com.dod.unrealzaruba.ModIntegrations.Xaero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsView;
import com.dod.unrealzaruba.UI.Objectives.HudCapturePointObjective;
import com.dod.unrealzaruba.UI.Objectives.HudDestroyObjective;
import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UI.Respawn.RespawnClientState;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

/**
 * Keeps Xaero's waypoints matching the match: one per objective and per non-objective respawn point, with the
 * selected respawn point marked. Instead of reacting to individual packets it compares the wanted waypoints with what
 * is in Xaero's current waypoint set every few ticks, so they come back after a reconnect, a dimension change, a new
 * Xaero session or the player deleting one. All of ours are temporary, so Xaero never saves them.
 */
public class XaeroWaypoints {
    static final ResourceKey<Level> GAME_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "game_dim"));
    private static final String SELECTED_PREFIX = "► ";

    private record Wanted(String key, BlockPos pos, String name, String initials, WaypointColor color, Byte respawnId) {
        String signature() {
            return pos.toShortString() + "|" + name + "|" + initials + "|" + color;
        }
    }

    private record Tracked(Waypoint waypoint, WaypointSet set, String signature) {}

    private final Map<String, Tracked> tracked = new HashMap<>();
    private final Map<Waypoint, Byte> respawnByWaypoint = new IdentityHashMap<>();
    private Object trackedSession;

    /** Returns true when waypoints were added or removed. */
    public boolean sync() {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session != trackedSession) {
            // A new session (reconnect) starts from its saved waypoints, where ours aren't, so forget the old objects
            tracked.clear();
            respawnByWaypoint.clear();
            trackedSession = session;
        }
        if (session == null) return false;

        MinimapWorld world = session.getWorldManager().getCurrentWorld();
        WaypointSet set = world == null ? null : world.getCurrentWaypointSet();
        List<Wanted> wanted = set != null && inGame(world) ? wanted() : List.of();

        Set<Waypoint> present = Collections.newSetFromMap(new IdentityHashMap<>());
        if (set != null) {
            set.getWaypoints().forEach(present::add);
        }

        boolean changed = false;
        Map<String, Wanted> byKey = new HashMap<>();
        for (Wanted w : wanted) {
            byKey.put(w.key(), w);
        }

        // Drop what is no longer wanted, outdated, in another set, or was deleted by the player
        for (var it = tracked.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Wanted w = byKey.get(entry.getKey());
            Tracked t = entry.getValue();
            boolean valid = w != null && t.set() == set && present.contains(t.waypoint()) && t.signature().equals(w.signature());
            if (!valid) {
                t.set().remove(t.waypoint());
                respawnByWaypoint.remove(t.waypoint());
                it.remove();
                changed = true;
            }
        }

        for (Wanted w : wanted) {
            if (tracked.containsKey(w.key())) continue;
            Waypoint waypoint = new Waypoint(w.pos().getX(), w.pos().getY(), w.pos().getZ(), w.name(), w.initials(),
                    w.color(), WaypointPurpose.NORMAL, true);
            waypoint.setTemporary(true);
            set.add(waypoint);
            tracked.put(w.key(), new Tracked(waypoint, set, w.signature()));
            if (w.respawnId() != null) {
                respawnByWaypoint.put(waypoint, w.respawnId());
            }
            changed = true;
        }
        return changed;
    }

    /** Respawn point id behind one of our waypoints (the minimap waypoint the world map shows), or null. */
    public Byte respawnIdFor(Object minimapWaypoint) {
        return minimapWaypoint instanceof Waypoint waypoint ? respawnByWaypoint.get(waypoint) : null;
    }

    public boolean isOurs(Object minimapWaypoint) {
        return minimapWaypoint instanceof Waypoint waypoint
                && tracked.values().stream().anyMatch(t -> t.waypoint() == waypoint);
    }

    private static boolean inGame(MinimapWorld world) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().equals(GAME_DIMENSION)) return false;
        return world.getDimId() == null || world.getDimId().equals(GAME_DIMENSION);
    }

    private static List<Wanted> wanted() {
        RespawnPointsView view = RespawnClientState.view();
        Byte effective = view.effectiveId();
        List<Wanted> wanted = new ArrayList<>();

        for (HudObjective objective : ObjectivesOverlay.INSTANCE.objectives()) {
            if (objective instanceof HudCapturePointObjective point) {
                RespawnPointsView.Entry respawn = view.points().stream()
                        .filter(entry -> entry.objectiveId() != null && entry.objectiveId() == point.GetRuntimeId())
                        .findFirst().orElse(null);
                boolean selected = respawn != null && effective != null && respawn.id() == effective;
                String letter = ObjectivesOverlay.INSTANCE.letterFor(point.GetRuntimeId());
                wanted.add(new Wanted("objective:" + point.GetRuntimeId(), point.GetPosition(),
                        (selected ? SELECTED_PREFIX : "") + point.GetName(),
                        letter != null ? letter : initial(point.GetName()),
                        selected ? WaypointColor.GOLD : teamColor(point.getOwner()),
                        respawn == null ? null : respawn.id()));
            } else if (objective instanceof HudDestroyObjective target) {
                wanted.add(new Wanted("objective:" + target.GetRuntimeId(), target.GetPosition(), target.GetName(),
                        initial(target.GetName()), WaypointColor.BLUE, null));
            }
        }

        for (RespawnPointsView.Entry entry : view.points()) {
            if (entry.kind() == RespawnPointsView.Kind.CAPTURE_POINT) continue; // shown on the objective's waypoint
            boolean selected = effective != null && entry.id() == effective;
            wanted.add(new Wanted("respawn:" + entry.id(), entry.position().above(),
                    (selected ? SELECTED_PREFIX : "") + entry.name(), kindInitial(entry.kind()),
                    selected ? WaypointColor.GOLD : WaypointColor.GREEN, entry.id()));
        }
        return wanted;
    }

    private static String initial(String name) {
        return name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
    }

    private static String kindInitial(RespawnPointsView.Kind kind) {
        return switch (kind) {
            case BASE -> "B";
            case TENT -> "T";
            case VEHICLE -> "V";
            case CAPTURE_POINT -> "C";
        };
    }

    private static WaypointColor teamColor(TeamColor team) {
        if (team == null) return WaypointColor.GRAY;
        return switch (team) {
            case RED -> WaypointColor.RED;
            case BLUE -> WaypointColor.BLUE;
            case YELLOW -> WaypointColor.YELLOW;
            case PURPLE -> WaypointColor.PURPLE;
            default -> WaypointColor.GRAY;
        };
    }
}
