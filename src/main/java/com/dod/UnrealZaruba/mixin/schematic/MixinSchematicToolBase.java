package com.dod.UnrealZaruba.mixin.schematic;

import com.simibubi.create.content.schematics.client.tools.SchematicToolBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SchematicToolBase.class)
public abstract class MixinSchematicToolBase {

    public MixinSchematicToolBase() {
    }

    @Redirect(
            method = "updateTargetPos()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/BlockHitResult;getLocation()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            )
    )
    public Vec3 redirectGetLocation(BlockHitResult blockHitResult) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        return new Vec3((double) blockPos.getX() + 0.5, (double) blockPos.getY() + 0.5, (double) blockPos.getZ() + 0.5);
    }
}
