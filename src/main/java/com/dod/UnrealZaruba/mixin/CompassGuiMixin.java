package com.dod.UnrealZaruba.mixin;

import dlovin.advancedcompass.gui.CompassGui;
import dlovin.advancedcompass.gui.renderers.WaypointRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompassGui.class)
public interface CompassGuiMixin {

    @Accessor("waypointRenderer")
    WaypointRenderer getWaypointRenderer();
}
