package com.dod.UnrealZaruba.Renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;

public interface Renderable {
    public void Render(PoseStack poseStack, Camera camera);
    public int GetId();
}
