package com.dod.unrealzaruba.Renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class ColoredSquareZone implements Renderable {
    AABB zone;
    int color;
    static float START_FADE_VALUE = 0.3f;
    public static int RenderableIDShift = 1;

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
        // set the depth state explicitly: other renderers at this stage (Sable, Veil...) can leave the depth test off,
        // and the zone then shows through blocks; translucent, so it does not write depth itself
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);


        // --- Bottom (full) ---
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, 0.3f);
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, 0.3f);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, 0.3f);
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, 0.3f);

        // --- North border (minZ) ---
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, START_FADE_VALUE);
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, START_FADE_VALUE);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, 0.0f);
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, 0.0f);

        // --- South border (maxZ) ---
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, START_FADE_VALUE);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, START_FADE_VALUE);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, 0.0f);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, 0.0f);

        // --- West border (minX) ---
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, START_FADE_VALUE);
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, START_FADE_VALUE);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, 0.0f);
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, 0.0f);

        // --- East border (maxX) ---
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, START_FADE_VALUE);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, START_FADE_VALUE);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, 0.0f);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, 0.0f);

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @Override
    public int GetId() {
        return zone.hashCode() + RenderableIDShift;
    }
}
