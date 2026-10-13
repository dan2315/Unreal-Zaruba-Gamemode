package com.dod.unrealzaruba.ModBlocks.Parachute;

import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * Draws the canopy and its lines above a deployed {@link ParachutePackBlockEntity}: the canopy model
 * (models/block/parachute_canopy.json, registered as a standalone model) scaled up, hanging a few blocks above the
 * pack and swaying a little. It opens by growing out of the pack. Works inside Sable sub-levels like any block entity
 * renderer, so the canopy follows the body.
 */
public class ParachutePackRenderer implements BlockEntityRenderer<ParachutePackBlockEntity> {
    public static final ModelResourceLocation CANOPY =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "block/parachute_canopy"));

    /** The model is about 3 blocks wide; this makes the canopy roughly 7 blocks across. */
    private static final float SCALE = 2.6f;
    /** Height of the canopy's rim above the top of the pack when fully open. */
    private static final float LINE_LENGTH = 3.3f;
    /** Half the model's width at its rim (in model blocks), where the lines are tied. */
    private static final float RIM_HALF_WIDTH = 22f / 16f;
    private static final double PACK_TOP = 0.25;
    /** How far from the block the canopy can reach, for culling. */
    private static final double REACH = 9;

    @SuppressWarnings("unused")
    public ParachutePackRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ParachutePackBlockEntity pack, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        float open = pack.openAmount(partialTick);
        if (open <= 0.001f || pack.getLevel() == null) return;
        float eased = open * open * (3 - 2 * open);

        float time = pack.getLevel().getGameTime() + partialTick;
        float swayX = (float) Math.sin(time * 0.06 + pack.getBlockPos().getX()) * 2.0f * eased;
        float swayZ = (float) Math.cos(time * 0.05 + pack.getBlockPos().getZ()) * 2.0f * eased;
        float rimHeight = (float) PACK_TOP + LINE_LENGTH * eased;

        BakedModel canopy = Minecraft.getInstance().getModelManager().getModel(CANOPY);
        poseStack.pushPose();
        poseStack.translate(0.5, rimHeight, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(swayX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(swayZ));
        poseStack.scale(SCALE * eased, SCALE * eased, SCALE * eased);
        poseStack.translate(-0.5, 0, -0.5);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                buffers.getBuffer(RenderType.cutout()), null, canopy, 1f, 1f, 1f, light, overlay);
        poseStack.popPose();

        // Lines from the pack to the four corners of the canopy's rim, following the sway
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();
        float reach = RIM_HALF_WIDTH * SCALE * eased;
        for (int i = 0; i < 4; i++) {
            float sx = (i & 1) == 0 ? -1 : 1;
            float sz = (i & 2) == 0 ? -1 : 1;
            float x = 0.5f + sx * reach;
            float z = 0.5f + sz * reach;
            float y = rimHeight;
            float dx = 0.5f - x, dy = (float) PACK_TOP - y, dz = 0.5f - z;
            float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0e-4f) continue;
            float nx = dx / length, ny = dy / length, nz = dz / length;
            lines.addVertex(pose, x, y, z).setColor(236, 232, 218, 255).setNormal(pose, nx, ny, nz);
            lines.addVertex(pose, 0.5f, (float) PACK_TOP, 0.5f).setColor(236, 232, 218, 255).setNormal(pose, nx, ny, nz);
        }
    }

    @Override
    public AABB getRenderBoundingBox(ParachutePackBlockEntity pack) {
        // The default is the pack's own block, so the canopy would vanish as soon as the pack leaves the screen
        return new AABB(pack.getBlockPos()).inflate(REACH, 0, REACH).expandTowards(0, REACH, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(ParachutePackBlockEntity pack) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
