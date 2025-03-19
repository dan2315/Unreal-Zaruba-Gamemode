package com.dod.UnrealZaruba.Mobs.MobRenderers;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Mobs.ModMobs;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UnrealZaruba.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobRenderersRegistration {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModMobs.CLICKABLE_HUMANOID_ENTITY.get(), EnterUnrealZarubaEntityRenderer::new);
    }
}
