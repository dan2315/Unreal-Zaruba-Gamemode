package com.dod.unrealzaruba.Schematics;

import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.UiFont;
import com.dod.unrealzaruba.ModBlocks.VehiclePurchase.VehiclePurchaseBlockEntity;
import com.dod.unrealzaruba.UnrealZaruba;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
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
 * Over a vehicle purchase block while a joint purchase goes on there: the name of the vehicle on the first line and
 * how much is paid of the price on the second. A block with no purchase going carries a plain "Vehicle spawn" label.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class PurchaseBlockLabels {
    private static final double MAX_DISTANCE = 64;
    private static final float SCALE = 0.03f;
    private static final double LIFT = 1.55;

    private static List<BlockPos> idle = List.of();
    private static long idleScannedAt = Long.MIN_VALUE;

    private PurchaseBlockLabels() {}

    /** The purchase blocks near the player, looked up now and then (the loaded chunks are walked, not every frame). */
    private static List<BlockPos> idleBlocks(Minecraft minecraft) {
        long now = minecraft.level.getGameTime();
        if (now - idleScannedAt < 20 && now >= idleScannedAt) return idle;
        idleScannedAt = now;
        List<BlockPos> found = new ArrayList<>();
        ChunkPos center = minecraft.player.chunkPosition();
        int radius = (int) (MAX_DISTANCE / 16) + 1;
        for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
            for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
                LevelChunk chunk = minecraft.level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof VehiclePurchaseBlockEntity && PurchaseClientState.get(entity.getBlockPos()) == null) found.add(entity.getBlockPos());
                }
            }
        }
        idle = found;
        return found;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        Font font = UiFont.get();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();

        for (BlockPos pos : idleBlocks(minecraft)) {
            double dx = pos.getX() + 0.5 - eye.x;
            double dy = pos.getY() + LIFT - eye.y;
            double dz = pos.getZ() + 0.5 - eye.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > MAX_DISTANCE) continue;
            float scale = SCALE * Mth.clamp((float) (distance / 10.0), 1f, 3f);
            pose.pushPose();
            pose.translate(dx, dy, dz);
            pose.mulPose(camera.rotation());
            pose.scale(-scale, -scale, scale);
            Matrix4f matrix = new Matrix4f(event.getModelViewMatrix()).mul(pose.last().pose());
            Component label = Component.translatable("schematics.unrealzaruba.purchase.block_label").withStyle(UiFont.bold());
            font.drawInBatch(label, -font.width(label) / 2f, 0, Brand.TEXT, false, matrix, buffers,
                    Font.DisplayMode.SEE_THROUGH, 0x66000000, 0xF000F0);
            pose.popPose();
        }

        for (PurchaseClientState.View view : PurchaseClientState.all()) {
            double dx = view.pos().getX() + 0.5 - eye.x;
            double dy = view.pos().getY() + LIFT - eye.y;
            double dz = view.pos().getZ() + 0.5 - eye.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > MAX_DISTANCE) continue;

            float scale = SCALE * Mth.clamp((float) (distance / 10.0), 1f, 3f);
            pose.pushPose();
            pose.translate(dx, dy, dz);
            pose.mulPose(camera.rotation());
            pose.scale(-scale, -scale, scale);
            // The level stage hands over the pose without the camera view, so it is added here
            Matrix4f matrix = new Matrix4f(event.getModelViewMatrix()).mul(pose.last().pose());

            Component name = Component.literal(view.name()).withStyle(UiFont.bold());
            font.drawInBatch(name, -font.width(name) / 2f, 0, Brand.TEXT, false, matrix, buffers,
                    Font.DisplayMode.SEE_THROUGH, 0x66000000, 0xF000F0);
            Component paid = Component.literal(Money.format(view.paid()) + " / " + Money.format(view.price()));
            font.drawInBatch(paid, -font.width(paid) / 2f, 12, Brand.CYAN, false, matrix, buffers,
                    Font.DisplayMode.SEE_THROUGH, 0x66000000, 0xF000F0);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
