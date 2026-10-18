package com.dod.unrealzaruba.UI.TabList;

import com.dod.unrealzaruba.UnrealZaruba;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** The vanilla player list is not drawn: {@link TabListOverlay} takes its place. */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class TabListGuard {
    private TabListGuard() {}

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.TAB_LIST)) event.setCanceled(true);
    }
}
