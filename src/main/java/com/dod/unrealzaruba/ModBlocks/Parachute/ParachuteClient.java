package com.dod.unrealzaruba.ModBlocks.Parachute;

import com.dod.unrealzaruba.ModBlocks.ModBlocks;
import com.dod.unrealzaruba.UnrealZaruba;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-only registration for the parachute canopy: its standalone model and the pack's block entity renderer. */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class ParachuteClient {
    private ParachuteClient() {}

    @SubscribeEvent
    public static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        event.register(ParachutePackRenderer.CANOPY);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.PARACHUTE_PACK_BLOCK_ENTITY.get(), ParachutePackRenderer::new);
    }
}
