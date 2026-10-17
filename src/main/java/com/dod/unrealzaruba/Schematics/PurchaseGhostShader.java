package com.dod.unrealzaruba.Schematics;

import java.io.IOException;

import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** The shader that draws the vehicle being bought: a ghost with the paid part tinted (assets/unrealzaruba/shaders/core/purchase_ghost). */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class PurchaseGhostShader {
    private static volatile ShaderInstance shader;

    private PurchaseGhostShader() {}

    /** Null when the shader could not be loaded. */
    public static ShaderInstance get() {
        return shader;
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "purchase_ghost"), DefaultVertexFormat.BLOCK),
                    loaded -> shader = loaded);
        } catch (IOException | RuntimeException e) {
            UnrealZaruba.LOGGER.warn("[PurchaseGhost] The ghost shader could not be loaded, the ghost is not drawn: {}", e.toString());
        }
    }
}
