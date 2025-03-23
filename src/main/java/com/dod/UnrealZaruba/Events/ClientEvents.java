package com.dod.UnrealZaruba.Events;

import com.dod.UnrealZaruba.UI.TimerOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.dod.UnrealZaruba.UnrealZaruba.LOGGER;
import static com.dod.UnrealZaruba.UnrealZaruba.MOD_ID;


/**
 * Все клиент-side ивенты сюда наъуй
 */
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    // Command registration is now centralized in CommandRegistration class

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        LOGGER.info("[Во, бля] Рисуюсь нахуй");

        event.registerAboveAll("gametimer", TimerOverlay.TIMER_OVERLAY);
    }
}
