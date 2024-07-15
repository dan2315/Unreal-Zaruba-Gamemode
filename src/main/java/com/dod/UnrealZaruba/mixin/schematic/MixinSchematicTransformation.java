package com.dod.UnrealZaruba.mixin.schematic;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSClientGameUtils;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(
        value = {SchematicTransformation.class},
        remap = false
)
public abstract class MixinSchematicTransformation {

    @Shadow
    private BlockPos target;

    @Shadow
    private Vec3 chasingPos;

    @Shadow
    private Vec3 prevChasingPos;

    public MixinSchematicTransformation() {
    }

    @Redirect(
            method = "applyTransformations(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/jozufozu/flywheel/util/transform/TransformStack;translate(Lnet/minecraft/world/phys/Vec3;)Ljava/lang/Object;",
                    ordinal = 0
            )
    )
    private Object redirectTranslate(TransformStack transformStack, Vec3 original) {
        PoseStack poseStack = (PoseStack)transformStack;
        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(Minecraft.getInstance().level, this.target.getX(), this.target.getY(), this.target.getZ());
        if (ship != null) {
            float partialTicks = AnimationTickHolder.getPartialTicks();
            Vec3 interpolatedPos = VecHelper.lerp(partialTicks, this.prevChasingPos, this.chasingPos);
            Vec3 cameraOffset = interpolatedPos.subtract(original);
            VSClientGameUtils.transformRenderWithShip(
                    ship.getTransform(),
                    poseStack,
                    interpolatedPos.x,
                    interpolatedPos.y,
                    interpolatedPos.z,
                    cameraOffset.x,
                    cameraOffset.y,
                    cameraOffset.z
            );
            return transformStack;
        } else {
            return transformStack.translate(original);
        }
    }
}
