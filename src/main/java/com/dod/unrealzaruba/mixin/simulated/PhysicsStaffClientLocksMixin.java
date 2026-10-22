package com.dod.unrealzaruba.mixin.simulated;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * Stopgap for a crash of Simulated's Physics Staff when it is clicked on a vehicle: {@code isLocked} (and {@code getLocks})
 * reads the list of locked vehicles of the level from a map, and the map has no entry until the server has sent the locks, so
 * {@code List.contains} meets a null. The map read is wrapped: no entry gives an empty list. Once Simulated fixes this itself
 * the wrapper just passes the real list through. If the methods change, the mixin quietly does nothing (the config is optional).
 */
@Mixin(targets = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler", remap = false)
public class PhysicsStaffClientLocksMixin {
    @WrapOperation(method = "isLocked", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object unrealzaruba$noLocksYet(Map<?, ?> map, Object key, Operation<Object> original) {
        Object locks = original.call(map, key);
        return locks == null ? List.of() : locks;
    }

    @WrapOperation(method = "getLocks", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object unrealzaruba$noLocksYetList(Map<?, ?> map, Object key, Operation<Object> original) {
        Object locks = original.call(map, key);
        return locks == null ? List.of() : locks;
    }
}
