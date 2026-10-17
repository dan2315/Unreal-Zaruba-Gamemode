package com.dod.unrealzaruba.mixin.simulated;

import java.util.List;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.Level;

/**
 * Stopgap for a crash of Simulated's Physics Staff: while one is held, {@code renderAllLocks} iterates the list that
 * {@code getLocks} returns, and that list is null when the level has no locks yet. Here a null becomes an empty list.
 * It only wraps the call, so once Simulated fixes this itself the wrapper just passes a real list through. If the
 * method or class changes, the mixin quietly does nothing (the config is optional and injectors need no match).
 */
@Mixin(targets = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffRenderHandler", remap = false)
public class PhysicsStaffLocksMixin {
    @WrapOperation(method = "renderAllLocks", at = @At(value = "INVOKE",
            target = "Ldev/simulated_team/simulated/content/physics_staff/PhysicsStaffClientHandler;getLocks(Lnet/minecraft/world/level/Level;)Ljava/util/List;"))
    private static List<UUID> unrealzaruba$nullLocks(Object handler, Level level, Operation<List<UUID>> original) {
        List<UUID> locks = original.call(handler, level);
        return locks != null ? locks : List.of();
    }
}
