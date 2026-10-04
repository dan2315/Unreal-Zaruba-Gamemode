package com.dod.unrealzaruba.Mobs.MobRenderers;

import com.dod.unrealzaruba.Mobs.ModMobs;

import com.dod.unrealzaruba.UnrealZaruba;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public class MobRenderersRegistration {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModMobs.CLICKABLE_HUMANOID_ENTITY.get(), EnterUnrealZarubaEntityRenderer::new);
    }
}
