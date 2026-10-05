package com.dod.unrealzaruba.mixin.xaero;

import com.dod.unrealzaruba.ModIntegrations.Xaero.XaeroWorldMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;

/** Non-OP players get no right-click menu on the empty map (Xaero's teleport, create waypoint, ...). */
@Mixin(value = GuiMap.class, remap = false)
public class GuiMapMixin {
    @Inject(method = "handleRightClick", at = @At("HEAD"), cancellable = true)
    private void unrealzaruba$noMapMenu(IRightClickableElement target, int x, int y, CallbackInfo ci) {
        if (target == (Object) this && !XaeroWorldMap.isOp()) {
            ci.cancel();
        }
    }
}
