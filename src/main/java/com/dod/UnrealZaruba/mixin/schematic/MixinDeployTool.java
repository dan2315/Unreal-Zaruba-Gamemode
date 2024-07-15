package com.dod.UnrealZaruba.mixin.schematic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.schematics.client.tools.DeployTool;
import com.simibubi.create.content.schematics.client.tools.SchematicToolBase;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSClientGameUtils;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(DeployTool.class)
public abstract class MixinDeployTool extends SchematicToolBase {

    public MixinDeployTool() {
    }

    @Redirect(
            method = "renderTool(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/simibubi/create/foundation/render/SuperRenderTypeBuffer;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
                    ordinal = 0
            )
    )
    public void redirectRenderTool(PoseStack poseStack, double originalX, double originalY, double originalZ) {
        float partialTicks = AnimationTickHolder.getPartialTicks();
        double interpolatedX = Mth.lerp((double) partialTicks, this.lastChasingSelectedPos.x, this.chasingSelectedPos.x);
        double interpolatedY = Mth.lerp((double) partialTicks, this.lastChasingSelectedPos.y, this.chasingSelectedPos.y);
        double interpolatedZ = Mth.lerp((double) partialTicks, this.lastChasingSelectedPos.z, this.chasingSelectedPos.z);

        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(Minecraft.getInstance().level, interpolatedX, interpolatedY, interpolatedZ);
        AABB schematicBounds = this.schematicHandler.getBounds();
        Vec3 schematicCenter = schematicBounds.getCenter();
        Vec3 interpolatedPos = new Vec3(interpolatedX, interpolatedY, interpolatedZ);

        schematicCenter = new Vec3((double) ((int) schematicCenter.x), 0.0, (double) ((int) schematicCenter.z));
        Vec3 cameraOffset = interpolatedPos.subtract(schematicCenter.add(new Vec3(originalX, originalY, originalZ)));
        int schematicCenterX = (int) schematicCenter.x;
        int schematicCenterZ = (int) schematicCenter.z;

        if (ship != null) {
            poseStack.pushPose();
            VSClientGameUtils.transformRenderWithShip(
                    ship.getTransform(),
                    poseStack,
                    interpolatedX - (double) schematicCenterX,
                    interpolatedY,
                    interpolatedZ - (double) schematicCenterZ,
                    cameraOffset.x,
                    cameraOffset.y,
                    cameraOffset.z
            );
        } else {
            poseStack.translate(
                    interpolatedX - (double) schematicCenterX - cameraOffset.x,
                    interpolatedY - cameraOffset.y,
                    interpolatedZ - (double) schematicCenterZ - cameraOffset.z
            );
        }
    }
}
