package com.dod.UnrealZaruba.Events;


import com.dod.UnrealZaruba.Config.MainConfig;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Все клиент-side ивенты сюда наъуй
 */
public class ClientEvents {
    private static boolean isDevMode = MainConfig.getInstance().getMode() == MainConfig.Mode.DEV;
    private GamemodeManager gamemodeManager;
    public ClientEvents(GamemodeManager gamemodeManager) {
        this.gamemodeManager = gamemodeManager;
    }

    @SubscribeEvent
    public void OnClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) {
            TimerManager.updateAll();
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ObjectivesOverlay.INSTANCE.Clear();
    }
}
