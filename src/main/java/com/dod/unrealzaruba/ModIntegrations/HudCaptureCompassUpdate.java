package com.dod.unrealzaruba.ModIntegrations;

import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.mixin.CompassGuiMixin;
import com.dod.unrealzaruba.mixin.WaypointRendererMixin;

import dlovin.advancedcompass.gui.CompassGui;
import dlovin.advancedcompass.gui.renderers.WaypointRenderer;
import dlovin.advancedcompass.utils.Color;
import dlovin.advancedcompass.utils.waypoints.Waypoint;
import dlovin.advancedcompass.utils.waypoints.WaypointIcon;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;


public class HudCaptureCompassUpdate {
    int minDistance = 0;
    int maxDistance = 1500;

    CompassGui advancedCompassGui = UnrealZaruba.advancedCompass.getCompassGui();
    WaypointRenderer waypointRenderer = ((CompassGuiMixin) advancedCompassGui).getWaypointRenderer();

    private final HashMap<Byte, Waypoint> waypointsMap = new HashMap<>();
    public List<Waypoint> getAllWaypoints() {
        return ((WaypointRendererMixin) waypointRenderer).getWaypoints();
    }

    public void UpdateOrCreateCompassWaypoints(byte id, String name, Vec3 position, int ownedByColor) {
        Waypoint newWaypoint = new Waypoint(name, position, "game_dim", WaypointIcon.RHOMBUS, new Color(ownedByColor), minDistance, maxDistance, true);
        if (waypointsMap.containsKey(id)) {
            advancedCompassGui.removeWaypoint(waypointsMap.get(id));
        }
        UnrealZaruba.LOGGER.info("Adding compass waypoint: {}", name);
        advancedCompassGui.addWaypoint(newWaypoint);
        waypointsMap.put(id, newWaypoint);
    }

    public boolean IsWaypointExists(Waypoint waypoint) {
        for (Waypoint wp : getAllWaypoints()) {
            if (waypoint.getPosition().equals(wp.getPosition())) {
                 return true;
            }
        }
        return false;
    }
}
