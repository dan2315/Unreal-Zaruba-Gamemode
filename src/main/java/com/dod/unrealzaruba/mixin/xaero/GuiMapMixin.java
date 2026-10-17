package com.dod.unrealzaruba.mixin.xaero;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dod.unrealzaruba.ModIntegrations.Xaero.XaeroWorldMap;

import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

/**
 * Players without OP get only the coordinates of the spot in the menu of the empty map: none of Xaero's own options
 * (waypoints, teleport, sharing, export, settings).
 */
@Mixin(value = GuiMap.class, remap = false)
public class GuiMapMixin {
    @Inject(method = "getRightClickOptions()Ljava/util/ArrayList;", at = @At("RETURN"), cancellable = true)
    private void unrealzaruba$coordinatesOnly(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        if (!XaeroWorldMap.isOp()) {
            cir.setReturnValue(XaeroWorldMap.coordinatesOnly(cir.getReturnValue(), (IRightClickableElement) this));
        }
    }
}
