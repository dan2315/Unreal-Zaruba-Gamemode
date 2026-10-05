package com.dod.unrealzaruba.mixin.xaero;

import java.util.ArrayList;

import com.dod.unrealzaruba.ModIntegrations.WaypointManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointReader;

/** Puts our respawn option into world map waypoint menus and hides Xaero's options from non-OP players. */
@Mixin(value = WaypointReader.class, remap = false)
public class WaypointReaderMixin {
    @Inject(method = "getRightClickOptions(Lxaero/map/mods/gui/Waypoint;Lxaero/map/gui/IRightClickableElement;)Ljava/util/ArrayList;",
            at = @At("RETURN"), cancellable = true)
    private void unrealzaruba$respawnOptions(Waypoint element, IRightClickableElement target,
                                             CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        var worldMap = WaypointManager.INSTANCE.worldMap();
        if (worldMap != null) {
            cir.setReturnValue(worldMap.waypointOptions(element, target, cir.getReturnValue()));
        }
    }
}
