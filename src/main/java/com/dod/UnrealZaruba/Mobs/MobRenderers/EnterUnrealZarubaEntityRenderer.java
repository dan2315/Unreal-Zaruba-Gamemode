package com.dod.UnrealZaruba.Mobs.MobRenderers;

import javax.annotation.Nonnull;

import com.dod.UnrealZaruba.Mobs.ClickableHumanoidEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class EnterUnrealZarubaEntityRenderer extends MobRenderer<ClickableHumanoidEntity, HumanoidModel<ClickableHumanoidEntity>> {

    public EnterUnrealZarubaEntityRenderer(Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(@Nonnull ClickableHumanoidEntity clickableEntity) {
        return new ResourceLocation("minecraft", "textures/entity/steve.png");
    }

    @Override
    public void render(ClickableHumanoidEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        // Render multiple labels above the entity
        renderLabels(entity, poseStack, buffer, packedLight);
    }

    // Render multiple labels
    private void renderLabels(Entity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Create multiple labels
        String[] labels = {"§4Unreal Zaruba", "100 players", "x2 points"};

        // Get the distance from the player to the entity (optional, for culling)
        double distance = Minecraft.getInstance().player.distanceTo(entity);
        
        // Distance check to prevent rendering at too far distances (optional)
        if (distance < 64) {
            poseStack.pushPose();
            poseStack.translate(0.0D, entity.getBbHeight() + 0.5D, 0.0D); // Position the text above the entity

            // Iterate over the labels and render them
            for (int i = 0; i < labels.length; i++) {
                // Adjust Y position for each label to stack them
                poseStack.pushPose();
                poseStack.translate(0.0D, i * 0.25D, 0.0D);  // Increase height for each subsequent label

                renderNameTagAboba(entity, Component.literal(labels[i]), poseStack, buffer, packedLight);

                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }

    // Helper method to render a single name tag
    private void renderNameTagAboba(Entity entity, Component label, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Font font = Minecraft.getInstance().font;
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        poseStack.mulPose(new Quaternionf().rotationYXZ(
                (float) Math.toRadians(-camera.getYRot()),
                (float) Math.toRadians(camera.getXRot()),
                0.0F
        ));

        float scale = 0.025F;
        poseStack.scale(-scale, -scale, scale); // Scale the text
        int yOffset = -8; // Offset the text above the entity

        font.drawInBatch(label.getString(), -font.width(label.getString()) / 2.0F, yOffset, 0xFFFFFF, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
    }
}
