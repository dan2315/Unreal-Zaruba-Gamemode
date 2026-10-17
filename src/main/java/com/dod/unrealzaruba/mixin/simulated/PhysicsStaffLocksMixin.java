package com.dod.unrealzaruba.mixin.simulated;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * Stopgap for a crash of Simulated's Physics Staff: while one is held, {@code renderAllLocks} walks the list that
 * {@code getLocks} returns, and that list is null when the level has no locks yet. The walk (the first
 * {@code List.iterator()} of the method) is wrapped: a null list gives an empty walk. Only the call is wrapped, so once
 * Simulated fixes this itself the wrapper just passes the real list through. If the method or class changes, the mixin
 * quietly does nothing (the config is optional and injectors need no match).
 *
 * The call is wrapped, not {@code getLocks}, because the mixin cannot name Simulated's own types here.
 */
@Mixin(targets = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffRenderHandler", remap = false)
public class PhysicsStaffLocksMixin {
    @WrapOperation(method = "renderAllLocks", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0))
    private static Iterator<?> unrealzaruba$nullLocks(List<?> locks, Operation<Iterator<?>> original) {
        return locks == null ? Collections.emptyIterator() : original.call(locks);
    }
}
