package com.dod.unrealzaruba.Renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;

public class GeometryRenderer {
    private HashMap<Integer ,Renderable> renderableMap = new HashMap<>();

    public void UpdateRenderableObject(Renderable renderable) {
        renderableMap.put(renderable.GetId(), renderable);
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();

        renderableMap.values().forEach(renderable -> {
            renderable.Render(poseStack, camera);
        });
    }
}
