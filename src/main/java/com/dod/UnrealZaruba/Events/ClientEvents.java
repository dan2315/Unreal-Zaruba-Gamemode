package com.dod.unrealzaruba.Events;


import com.dod.unrealzaruba.Config.MainConfig;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.UI.GamemodeSelectionScreen;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
    public void OnClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) {
            TimerManager.updateAll();
        }
        if (event.phase.equals(TickEvent.Phase.END)) {
            while (KeyBindings.OPEN_GAMEMODE_MENU.consumeClick()) {
                Minecraft.getInstance().setScreen(new GamemodeSelectionScreen(Component.literal("Выбор игрового режима")));
            }
        }
    }

    @SubscribeEvent
    public void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        UnrealZaruba.LOGGER.warn("Clearing objectives overlay");
        ObjectivesOverlay.INSTANCE.Clear();
    }
}
