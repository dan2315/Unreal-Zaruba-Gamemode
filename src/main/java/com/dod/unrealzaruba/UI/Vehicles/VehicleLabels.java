package com.dod.unrealzaruba.UI.Vehicles;

import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.UiFont;
import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The name of each of the team's vehicles over it, in the team's colour and visible through terrain, with the distance
 * under it. The size grows with the distance so a vehicle far away can still be read.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class VehicleLabels {
    private static final double MAX_DISTANCE = 600;
    private static final float BASE_SCALE = 0.025f;
    private static final double LIFT = 1.4;

    private VehicleLabels() {}

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        var vehicles = TeamVehiclesState.all();
        if (vehicles.isEmpty()) return;

        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        Font font = UiFont.get();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();

        for (TeamVehiclesState.Shown vehicle : vehicles) {
            double dx = vehicle.x() - eye.x;
            double dy = vehicle.topY() + LIFT - eye.y;
            double dz = vehicle.z() - eye.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > MAX_DISTANCE) continue;

            float scale = BASE_SCALE * Mth.clamp((float) (distance / 14.0), 1f, 9f);
            pose.pushPose();
            pose.translate(dx, dy, dz);
            pose.mulPose(camera.rotation());
            pose.scale(-scale, -scale, scale);

            int color = HudStyle.teamColor(vehicle.team());
            Component name = Component.literal(vehicle.name()).withStyle(UiFont.bold());
            font.drawInBatch(name, -font.width(name) / 2f, 0, color, false, pose.last().pose(), buffers,
                    Font.DisplayMode.SEE_THROUGH, 0x66000000, 0xF000F0);
            Component meters = Component.literal(Math.round(distance) + " m");
            pose.pushPose();
            pose.scale(0.75f, 0.75f, 1f);
            font.drawInBatch(meters, -font.width(meters) / 2f, 11, 0xFFD6DBDF, false, pose.last().pose(), buffers,
                    Font.DisplayMode.SEE_THROUGH, 0x66000000, 0xF000F0);
            pose.popPose();
            pose.popPose();
        }
        buffers.endBatch();
    }
}
