package com.dod.unrealzaruba.ModIntegrations;

import com.dod.unrealzaruba.UI.Objectives.HudCapturePointObjective;
import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import com.dod.unrealzaruba.utils.Conversion;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
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
                    capturePointObjective.GetName().substring(0, 1).toUpperCase(),
                    Conversion.IntColorToXaeroColor(capturePointObjective.getOwnerColor()),
                    WaypointPurpose.NORMAL,
                    true
            );
            waypointSet.add(wp);
            return wp;
        }
        return null;
    }

}
