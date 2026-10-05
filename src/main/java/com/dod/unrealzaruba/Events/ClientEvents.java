package com.dod.unrealzaruba.Events;


import com.dod.unrealzaruba.Config.MainConfig;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.UI.GamemodeSelectionScreen;
import com.dod.unrealzaruba.UI.MoneyOverlay;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import com.dod.unrealzaruba.NetworkPackets.Economy.RequestBuyMenuPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.neoforged.bus.api.SubscribeEvent;

/**
 * Все клиент-side ивенты сюда наъуй
 */
@OnlyIn(Dist.CLIENT)
public class ClientEvents {
    private static boolean isDevMode = MainConfig.getInstance().getMode() == MainConfig.Mode.DEV;
    private GamemodeManager gamemodeManager;
    public ClientEvents(GamemodeManager gamemodeManager) {
        this.gamemodeManager = gamemodeManager;
    }

    @SubscribeEvent
    public void OnClientTickPre(ClientTickEvent.Pre event) {
        TimerManager.updateAll();
    }

    @SubscribeEvent
    public void OnClientTickPost(ClientTickEvent.Post event) {
        while (KeyBindings.OPEN_GAMEMODE_MENU.consumeClick()) {
            Minecraft.getInstance().setScreen(new GamemodeSelectionScreen(Component.literal("Выбор игрового режима")));
        }
        while (KeyBindings.OPEN_BUY_MENU.consumeClick()) {
            if (Minecraft.getInstance().getConnection() != null) {
                PacketDistributor.sendToServer(new RequestBuyMenuPacket());
            }
        }
    }

    @SubscribeEvent
    public void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        UnrealZaruba.LOGGER.warn("Clearing objectives overlay");
        ObjectivesOverlay.INSTANCE.Clear();
        MoneyOverlay.INSTANCE.set(false, 0);
    }
}
