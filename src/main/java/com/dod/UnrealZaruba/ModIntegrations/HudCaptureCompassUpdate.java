package com.dod.UnrealZaruba.ModIntegrations;

import com.dod.UnrealZaruba.mixin.CompassGuiMixin;
import com.dod.UnrealZaruba.mixin.WaypointRendererMixin;

import dlovin.advancedcompass.gui.CompassGui;
import dlovin.advancedcompass.gui.renderers.WaypointRenderer;
import dlovin.advancedcompass.utils.Color;
import dlovin.advancedcompass.utils.waypoints.Waypoint;
import dlovin.advancedcompass.utils.waypoints.WaypointIcon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.dod.UnrealZaruba.UnrealZaruba.advancedCompass;

public class HudCaptureCompassUpdate {
    int minDistance = 0;
    int maxDistance = 1500;

    CompassGui advancedCompassGui = advancedCompass.getCompassGui();
    WaypointRenderer waypointRenderer = ((CompassGuiMixin) advancedCompassGui).getWaypointRenderer();

    public List<Waypoint> getAllWaypoints() {
        return ((WaypointRendererMixin) waypointRenderer).getWaypoints();
    }

    public void UpdateOrCreateCompassWaypoints(String name, Vec3 position, int capturedByColor, int ownedByColor) {
        Waypoint maybeOldWaypoint = new Waypoint(name, position, "game_dim", WaypointIcon.RHOMBUS, IntToColor(capturedByColor), minDistance, maxDistance, true);
        Waypoint newWaypoint = new Waypoint(name, position, "game_dim", WaypointIcon.RHOMBUS, IntToColor(ownedByColor), minDistance, maxDistance, true);
        if (IsWaypointExists(maybeOldWaypoint)) {
            advancedCompassGui.removeWaypoint(maybeOldWaypoint);
            advancedCompassGui.addWaypoint(newWaypoint);
        }
        advancedCompassGui.addWaypoint(newWaypoint);
    }

    public Vec3 Vec3iToVec3(Vec3i position) {
        return new Vec3(position.getX(), position.getY(), position.getZ());
    }

    public Vec3 BlockPosToVec3(BlockPos position) {
        return new Vec3(position.getX(), position.getY(), position.getZ());
    }

    public Color IntToColor(int motherfuckerDodsColor) {
        float red   = (motherfuckerDodsColor & 0xFF0000) >> 16; // Красный (0–255)
        float green = (motherfuckerDodsColor & 0x00FF00) >> 8;  // Зелёный (0–255)
        float blue  = (motherfuckerDodsColor & 0x0000FF);       // Синий (0–255)

        return new Color(red, green, blue);
    }

    public boolean IsWaypointExists(Waypoint waypoint) {
            for (Waypoint wp : getAllWaypoints()) {
                if (waypoint == wp) {
                     return true;
                }
            }
        return false;
    }
}
