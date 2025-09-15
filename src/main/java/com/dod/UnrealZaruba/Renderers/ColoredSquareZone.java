package com.dod.UnrealZaruba.Renderers;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ColoredSquareZone implements Renderable {
    AABB zone;
    int color;
    static float START_FADE_VALUE = 0.3f;
    static int RenderableIDShift = 1;

    public ColoredSquareZone(AABB zone, int color) {
        this.zone = zone;
        this.color = color;
    }

    public AABB getZone() {
        return zone;
    }

    public int getColor() {
        return color;
    }

    @Override
    public void Render(PoseStack poseStack, Camera camera) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        Vec3 camPos = camera.getPosition();
        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        float eps = 0.001f;

        float minX = (float) (zone.minX - camX) + eps;
        float minY = (float) (zone.minY - camY) + eps;
        float minZ = (float) (zone.minZ - camZ) + eps;
        float maxX = (float) (zone.maxX - camX) + 1 - eps;
        float maxY = (float) ((zone.minY + 1) - camY);
        float maxZ = (float) (zone.maxZ - camZ) + 1 - eps;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);


        // --- Bottom (full) ---
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, 0.3f).endVertex();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, 0.3f).endVertex();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, 0.3f).endVertex();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, 0.3f).endVertex();

        // --- North border (minZ) ---
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, START_FADE_VALUE).endVertex();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, START_FADE_VALUE).endVertex();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, 0.0f).endVertex();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, 0.0f).endVertex();

        // --- South border (maxZ) ---
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, START_FADE_VALUE).endVertex();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, START_FADE_VALUE).endVertex();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, 0.0f).endVertex();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, 0.0f).endVertex();

        // --- West border (minX) ---
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, START_FADE_VALUE).endVertex();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, START_FADE_VALUE).endVertex();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, 0.0f).endVertex();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, 0.0f).endVertex();

        // --- East border (maxX) ---
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, START_FADE_VALUE).endVertex();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, START_FADE_VALUE).endVertex();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, 0.0f).endVertex();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, 0.0f).endVertex();

        Tesselator.getInstance().end();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @Override
    public int GetId() {
        return zone.hashCode() + RenderableIDShift;
    }
}
