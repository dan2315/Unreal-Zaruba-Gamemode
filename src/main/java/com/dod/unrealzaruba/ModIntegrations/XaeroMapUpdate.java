package com.dod.unrealzaruba.ModIntegrations;

import com.dod.unrealzaruba.UI.Objectives.HudCapturePointObjective;
import com.dod.unrealzaruba.UI.Objectives.HudDestroyObjective;
import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.utils.Conversion;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;

import java.util.HashMap;
import java.util.List;

public class XaeroMapUpdate {
    private final MinimapSession session;
    HashMap<Byte, Waypoint> waypointsMap = new HashMap<>();

    public XaeroMapUpdate() {
        session = BuiltInHudModules.MINIMAP.getCurrentSession();
    }

    public void UpdateOrCreateMinimapWaypoints(List<HudObjective> objectives) {
        var world = session.getWorldManager().getCurrentWorld();
        var waypointSet = world.getWaypointSet("gui.xaero_default");

        for (var objective : objectives) {
            byte id = objective.GetRuntimeId();
            if (waypointsMap.containsKey(id)) {
                waypointSet.remove(waypointsMap.get(id));
                waypointsMap.remove(id);
            }
            waypointsMap.put(id, AddWaypointFor(objective, waypointSet));
        }

    }

    private Waypoint AddWaypointFor(HudObjective objective, WaypointSet waypointSet) {
        if (objective instanceof HudCapturePointObjective capturePointObjective) {
            var wp = new Waypoint(
                    capturePointObjective.GetPosition().getX(),
                    capturePointObjective.GetPosition().getY(),
                    capturePointObjective.GetPosition().getZ(),
                    capturePointObjective.GetName(),
                    initialsFor(capturePointObjective),
                    IntColorToXaeroColor(capturePointObjective.getOwnerColor()),
                    WaypointPurpose.NORMAL,
                    true
            );
            waypointSet.add(wp);
            return wp;
        } else if (objective instanceof HudDestroyObjective hudDestroyObjective) {
            var wp = new Waypoint(
                    hudDestroyObjective.GetPosition().getX(),
                    hudDestroyObjective.GetPosition().getY(),
                    hudDestroyObjective.GetPosition().getZ(),
                    hudDestroyObjective.GetName(),
                    hudDestroyObjective.GetName().substring(0, 1).toUpperCase(),
                    WaypointColor.BLUE,
                    WaypointPurpose.NORMAL,
                    true
            );
            waypointSet.add(wp);
            return wp;
        }
        return null;
    }


    // Same letter as the objectives panel, so the minimap pins match the rows
    private static String initialsFor(HudCapturePointObjective objective) {
        String letter = ObjectivesOverlay.INSTANCE.letterFor(objective.GetRuntimeId());
        return letter != null ? letter : objective.GetName().substring(0, 1).toUpperCase();
    }

    private static WaypointColor IntColorToXaeroColor(int color) {
        WaypointColor closest = null;
        double closestDistance = Double.MAX_VALUE;

        int r1 = (color >> 16) & 0xFF;
        int g1 = (color >> 8) & 0xFF;
        int b1 = color & 0xFF;

        for (WaypointColor wc : WaypointColor.values()) {
            int hex = wc.getHex();
            int r2 = (hex >> 16) & 0xFF;
            int g2 = (hex >> 8) & 0xFF;
            int b2 = hex & 0xFF;

            double distance = Math.sqrt(Math.pow(r1 - r2, 2) +
                    Math.pow(g1 - g2, 2) +
                    Math.pow(b1 - b2, 2));
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = wc;
            }
        }

        return closest;
    }
}
