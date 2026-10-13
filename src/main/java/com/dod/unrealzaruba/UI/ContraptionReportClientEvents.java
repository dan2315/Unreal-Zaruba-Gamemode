package com.dod.unrealzaruba.UI;

import com.dod.unrealzaruba.UnrealZaruba;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** The scanner panel belongs to a world: leaving it clears the panel. */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class ContraptionReportClientEvents {
    private ContraptionReportClientEvents() {}

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ContraptionReportOverlay.INSTANCE.set(null);
    }
}
