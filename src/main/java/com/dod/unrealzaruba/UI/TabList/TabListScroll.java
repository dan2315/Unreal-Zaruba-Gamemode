package com.dod.unrealzaruba.UI.TabList;

import com.dod.unrealzaruba.UI.Feed.FeedState;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * While TAB is held the mouse wheel scrolls the history of the event feed instead of changing the item in hand. Each notch is two lines.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class TabListScroll {
    private static final int LINES_PER_NOTCH = 2;

    private TabListScroll() {}

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.options.keyPlayerList.isDown()) return;
        if (FeedState.lines().isEmpty()) return;
        double delta = event.getScrollDeltaY();
        if (delta == 0) return;
        FeedState.scrollBy((delta > 0 ? 1 : -1) * LINES_PER_NOTCH, TabListOverlay.historyRows());
        event.setCanceled(true);
    }
}
