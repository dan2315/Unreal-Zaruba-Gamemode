package com.dod.unrealzaruba.Schematics;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.Vehicles.Blueprints;
import com.enxv.aeronauticsstructuretool.PreviewBlueprintData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The vehicle being bought, shown whole where it will appear, as a hologram: white, with the part the money covers so
 * far filled with light blue from the bottom up, like a progress bar, and a bright line where the fill ends. The
 * blueprint is read with the toolgun's public reader; the mesh ({@link HologramMesh}) and the shader
 * ({@link PurchaseGhostShader}) are ours.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class PurchaseGhostRenderer {
    private static final double MAX_DISTANCE = 160;
    private static final long RETRY_MILLIS = 30_000;
    private static final long BUILD_BUDGET_NANOS = 3_000_000;
    /** How fast the fill follows the paid share (per second): it glides instead of jumping at every payment. */
    private static final float GLIDE_PER_SECOND = 3.0f;

    /** A blueprint downloaded for a hologram, and the mesh made of it. */
    private static final class Entry {
        PreviewBlueprintData data;
        HologramMesh mesh;
        long failedAt;
        boolean loading;
    }

    private static final Map<Long, Entry> ENTRIES = new HashMap<>();
    private static final Map<BlockPos, Float> SHOWN = new HashMap<>();
    private static long lastFrame;
    private static String reported = "";
    private static long meshWaitSince;

    private PurchaseGhostRenderer() {}

    public static void reset() {
        for (Entry entry : ENTRIES.values()) {
            if (entry.mesh != null) entry.mesh.close();
        }
        ENTRIES.clear();
        SHOWN.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || PurchaseClientState.all().isEmpty()) return;
        ShaderInstance shader = PurchaseGhostShader.get();
        if (shader == null) {
            report(minecraft, "the hologram shader is not loaded");
            return;
        }

        long now = System.currentTimeMillis();
        float seconds = lastFrame == 0 ? 0 : Math.min(0.2f, (now - lastFrame) / 1000f);
        lastFrame = now;
        Vec3 camera = event.getCamera().getPosition();

        for (PurchaseClientState.View view : PurchaseClientState.all()) {
            if (camera.distanceToSqr(view.centerX(), view.minimumBlockCenterY(), view.centerZ()) > MAX_DISTANCE * MAX_DISTANCE) continue;
            Entry entry = entryFor(minecraft, view.schematicId());
            if (entry.data == null) continue;
            if (entry.mesh == null) entry.mesh = new HologramMesh(entry.data);
            HologramMesh mesh = entry.mesh;
            if (mesh.isEmpty()) {
                report(minecraft, "the blueprint has nothing to draw: " + mesh.partCount + " parts, no block models, no outline points");
                continue;
            }
            if (!mesh.advance(BUILD_BUDGET_NANOS)) {
                if (meshWaitSince == 0) meshWaitSince = now;
                if (now - meshWaitSince > 8000) report(minecraft, "the hologram mesh is slow to build");
                continue;
            }
            meshWaitSince = 0;
            if (mesh.buffer() == null) {
                report(minecraft, "the hologram mesh came out empty (" + mesh.blockCount + " blocks, " + mesh.samplePoints + " outline points)");
                continue;
            }

            float target = view.fraction();
            float shown = SHOWN.getOrDefault(view.pos(), 0f);
            shown += (target - shown) * Math.min(1f, GLIDE_PER_SECOND * seconds);
            if (Math.abs(target - shown) < 0.002f) shown = target;
            SHOWN.put(view.pos(), shown);

            // Stood where the vehicle will be placed: its lowest block centre at the placement height
            double originY = view.minimumBlockCenterY() - mesh.lowestCenterY();
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(view.centerX() - camera.x, originY - camera.y, view.centerZ() - camera.z);
            pose.mulPose(new Quaternionf(entry.data.rootOrientation()));
            try {
                draw(shader, mesh, new Matrix4f(event.getModelViewMatrix()).mul(pose.last().pose()),
                        event.getProjectionMatrix(), shown);
            } finally {
                pose.popPose();
            }
        }
    }

    private static void draw(ShaderInstance shader, HologramMesh mesh, Matrix4f modelView, Matrix4f projection, float fraction) {
        VertexBuffer buffer = mesh.buffer();
        double fillHeight = mesh.minY() + Mth.clamp(fraction, 0f, 1f) * (mesh.maxY() - mesh.minY());

        float[] color = RenderSystem.getShaderColor().clone();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.setShader(() -> shader);
        try {
            shader.safeGetUniform("GhostTime").set((System.currentTimeMillis() % 100_000L) / 1000f);
            shader.safeGetUniform("FillHeight").set((float) fillHeight);
            shader.safeGetUniform("FillSoft").set((float) Math.max(0.25, (mesh.maxY() - mesh.minY()) * 0.03));
            buffer.bind();
            buffer.drawWithShader(modelView, projection, shader);
            VertexBuffer.unbind();
        } finally {
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    private static Entry entryFor(Minecraft minecraft, long schematicId) {
        Entry entry = ENTRIES.computeIfAbsent(schematicId, id -> new Entry());
        if (entry.data != null || entry.loading) return entry;
        if (entry.failedAt != 0 && System.currentTimeMillis() - entry.failedAt < RETRY_MILLIS) return entry;
        if (!SchematicsApi.isLoggedIn()) {
            report(minecraft, "no hologram: not signed in to the backend, the blueprint cannot be downloaded");
            return entry;
        }
        entry.loading = true;
        SchematicsApi.then(SchematicsApi.download(schematicId), result -> {
            entry.loading = false;
            try {
                if (!result.ok()) throw new IllegalStateException(String.valueOf(result.error()));
                entry.data = PreviewBlueprintData.parse(Blueprints.withoutSavedRotation(result.value()), Minecraft.getInstance().level);
            } catch (Exception e) {
                entry.failedAt = System.currentTimeMillis();
                report(Minecraft.getInstance(), "no hologram for schematic " + schematicId + ": " + e);
            }
        });
        return entry;
    }

    /** Said once in the chat of the player, since the game log of a launched client is not at hand. */
    private static void report(Minecraft minecraft, String problem) {
        if (problem.equals(reported)) return;
        reported = problem;
        UnrealZaruba.LOGGER.warn("[PurchaseGhost] {}", problem);
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("[Ghost] " + problem), false);
        }
    }
}
