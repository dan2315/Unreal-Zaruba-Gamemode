package com.dod.unrealzaruba.Economy;

import java.util.List;

import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundSupplyCasePacket;
import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * A beacon-style column of light, in the colour of the team that last held it, over every supply case that is carried
 * or lying on the ground. Drawn by hand so it shows at any distance and follows the carrier smoothly.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class SupplyCaseBeams {
    private static final float BEAM_RADIUS = 0.3f;
    private static final float GLOW_RADIUS = 0.4f;

    private SupplyCaseBeams() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        List<ClientboundSupplyCasePacket.Entry> entries = SupplyCaseClientState.entries();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (entries.isEmpty() || level == null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        for (ClientboundSupplyCasePacket.Entry entry : entries) {
            // The carrier doesn't get a column of light through their own view; everyone else does
            if (minecraft.player != null && entry.entityId() == minecraft.player.getId()) continue;
            Entity entity = level.getEntity(entry.entityId());
            double x = entity != null ? Mth.lerp(partialTick, entity.xo, entity.getX()) : entry.x();
            double y = entity != null ? Mth.lerp(partialTick, entity.yo, entity.getY()) : entry.y();
            double z = entity != null ? Mth.lerp(partialTick, entity.zo, entity.getZ()) : entry.z();

            int height = Math.max(16, level.getMaxBuildHeight() + 64 - (int) y);
            pose.pushPose();
            pose.translate(x - camera.x, y - camera.y, z - camera.z);
            BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0f,
                    level.getGameTime(), 0, height, 0xFF000000 | entry.color(), BEAM_RADIUS, GLOW_RADIUS);
            pose.popPose();
        }
        buffers.endBatch(RenderType.beaconBeam(BeaconRenderer.BEAM_LOCATION, false));
        buffers.endBatch(RenderType.beaconBeam(BeaconRenderer.BEAM_LOCATION, true));
    }
}
