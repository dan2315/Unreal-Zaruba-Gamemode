package com.dod.UnrealZaruba.mixin;

import dlovin.advancedcompass.gui.renderers.WaypointRenderer;
import dlovin.advancedcompass.utils.waypoints.Waypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(WaypointRenderer.class)
public interface WaypointRendererMixin {

    @Accessor("waypoints")
    List<Waypoint> getWaypoints();
}
