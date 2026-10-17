package com.dod.unrealzaruba.UI.Respawn;

import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * On the game map the server respawns a player who died at once and keeps them waiting as a spectator, with the map
 * open: the vanilla death screen would only flash up in between, so it is not opened there.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class DeathScreenGuard {
    private static final ResourceLocation GAME_DIM = ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "game_dim");

    private DeathScreenGuard() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof DeathScreen)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && GAME_DIM.equals(minecraft.level.dimension().location())) {
            event.setCanceled(true);
        }
    }
}
