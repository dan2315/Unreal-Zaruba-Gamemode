package com.dod.UnrealZaruba.mixin;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.config.VSGameConfig;
import org.valkyrienskies.mod.common.util.GameTickForceApplier;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.dod.UnrealZaruba.unrealzaruba;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Explosion.class, priority = 999)
public abstract class MixinExplosionMixin {

    @Shadow
    @Final
    private Level level;

    @Shadow
    @Final
    @Mutable
    private double x;

    @Shadow
    @Final
    @Mutable
    private double y;
    @Shadow
    @Final
    @Mutable
    private double z;
    @Shadow
    @Final
    @Mutable
    private float radius;

    @Inject(method = "doExplodeForce", at = @At("HEAD"), cancellable = true)
    private void replaceDoExplodeForce(CallbackInfo ci) {
        final Vector3d originPos = new Vector3d(this.x, this.y, this.z);
        final BlockPos explodePos = new BlockPos(originPos.x(), originPos.y(), originPos.z());
        final int radius = (int) Math.ceil(this.radius);
        for (int x = radius; x >= -radius; x--) {
            for (int y = radius; y >= -radius; y--) {
                for (int z = radius; z >= -radius; z--) {
                    final BlockHitResult result = level.clip(
                            new ClipContext(Vec3.atCenterOf(explodePos),
                                    Vec3.atCenterOf(explodePos.offset(x, y, z)),
                                    ClipContext.Block.COLLIDER,
                                    ClipContext.Fluid.NONE, null));
                    if (result.getType() == HitResult.Type.BLOCK) {
                        final BlockPos blockPos = result.getBlockPos();
                        if (VSGameUtilsKt.getShipObjectManagingPos(this.level,
                                blockPos) instanceof ServerShip serverShipObject) {

                            if (serverShipObject != null) {
                                final Vector3d forceVector = VectorConversionsMCKt.toJOML(
                                        Vec3.atCenterOf(explodePos)); // Start at center position
                                final Double distanceMult = Math.max(0.5, 1.0 - (this.radius /
                                        forceVector.distance(VectorConversionsMCKt.toJOML(Vec3.atCenterOf(blockPos)))));
                                final Double powerMult = Math.max(0.1, this.radius / 4); // TNT blast radius = 4

                                forceVector.sub(VectorConversionsMCKt.toJOML(
                                        Vec3.atCenterOf(blockPos))); // Subtract hit block pos to get direction
                                forceVector.normalize();
                                forceVector.mul(-1 *
                                        VSGameConfig.SERVER.getExplosionBlastForce()); // Multiply by blast force at
                                                                                       // center
                                                                                       // position. Negative because of
                                                                                       // how
                                                                                       // we got the direction.
                                forceVector.mul(distanceMult); // Multiply by distance falloff
                                forceVector.mul(powerMult); // Multiply by radius, roughly equivalent to power

                                final GameTickForceApplier forceApplier = serverShipObject
                                        .getAttachment(GameTickForceApplier.class);
                                final Vector3dc shipCoords = serverShipObject.getShipTransform()
                                        .getShipPositionInShipCoordinates();
                                if (forceVector.isFinite()) {
                                    forceApplier.applyInvariantForceToPos(forceVector,
                                            VectorConversionsMCKt.toJOML(Vec3.atCenterOf(blockPos)).sub(shipCoords));
                                }
                            }
                        }

                    }
                }
            }
        }
        ci.cancel();
    }
}
