package com.dod.unrealzaruba.mixin.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.dod.unrealzaruba.ModIntegrations.WaypointManager;
import com.dod.unrealzaruba.ModIntegrations.Xaero.XaeroWorldMap;

import xaero.map.MapProcessor;

/**
 * The level id Xaero's server part sends picks the folder Xaero reads a dimension's map from. In the game dimension
 * it is swapped for the id of the running map's pack, so entering the dimension opens that map straight away.
 */
@Mixin(value = MapProcessor.class, remap = false)
public class MapProcessorMixin {
    @ModifyVariable(method = "onServerLevelId(I)V", at = @At("HEAD"), argsOnly = true)
    private int unrealzaruba$packLevelId(int id) {
        XaeroWorldMap worldMap = WaypointManager.INSTANCE.worldMap();
        return worldMap == null ? id : worldMap.onServerLevelId(id);
    }
}
