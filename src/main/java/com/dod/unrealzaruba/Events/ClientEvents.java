package com.dod.unrealzaruba.Events;


import com.dod.unrealzaruba.Config.MainConfig;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.UI.GamemodeSelectionScreen;
import com.dod.unrealzaruba.UI.MoneyOverlay;
import com.dod.unrealzaruba.ModIntegrations.WaypointManager;
import com.dod.unrealzaruba.ModIntegrations.Xaero.MapPackClient;
import com.dod.unrealzaruba.UI.Respawn.RespawnClientState;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.Schematics.SchematicsBrowserScreen;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import com.dod.unrealzaruba.NetworkPackets.Economy.RequestBuyMenuPacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.RequestTeamStoragePacket;
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
        WaypointManager.INSTANCE.tick();
        MapPackClient.tick();
    }

    private boolean wasInGame;

    @SubscribeEvent
    public void OnClientTickPost(ClientTickEvent.Post event) {
        // Leaving the game world (the match ended, back to the lobby): the match HUD goes with it
        var level = Minecraft.getInstance().level;
        if (level != null) {
            boolean inGame = level.dimension().location().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "game_dim"));
            if (wasInGame && !inGame) com.dod.unrealzaruba.UI.MatchHud.resetAll();
            wasInGame = inGame;
        }
        while (KeyBindings.OPEN_GAMEMODE_MENU.consumeClick()) {
            Minecraft.getInstance().setScreen(new GamemodeSelectionScreen(Component.literal("Выбор игрового режима")));
        }
        // Held beside an overturned vehicle: the server counts the ticks
        if (KeyBindings.FLIP_VEHICLE.isDown() && Minecraft.getInstance().screen == null && Minecraft.getInstance().getConnection() != null
                && Minecraft.getInstance().player != null && !Minecraft.getInstance().player.isSpectator()) {
            PacketDistributor.sendToServer(new com.dod.unrealzaruba.NetworkPackets.Vehicles.FlipVehiclePacket());
        }
        while (KeyBindings.READY.consumeClick()) {
            if (Minecraft.getInstance().getConnection() != null && com.dod.unrealzaruba.UI.Lobby.LobbyClientState.active()) {
                PacketDistributor.sendToServer(new com.dod.unrealzaruba.NetworkPackets.Lobby.ToggleReadyPacket());
            }
        }
        while (KeyBindings.OPEN_BUY_MENU.consumeClick()) {
            if (Minecraft.getInstance().getConnection() != null) {
                PacketDistributor.sendToServer(new RequestBuyMenuPacket());
            }
        }
        while (KeyBindings.OPEN_TEAM_STORAGE.consumeClick()) {
            if (Minecraft.getInstance().getConnection() != null) {
                PacketDistributor.sendToServer(new RequestTeamStoragePacket());
            }
        }
        while (KeyBindings.OPEN_SCHEMATICS.consumeClick()) {
            Minecraft.getInstance().setScreen(new SchematicsBrowserScreen());
        }
    }

    @SubscribeEvent
    public void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        UnrealZaruba.LOGGER.warn("Clearing objectives overlay");
        com.dod.unrealzaruba.UI.MatchHud.resetAll();
        wasInGame = false;
        MoneyOverlay.INSTANCE.set(false, 0);
        com.dod.unrealzaruba.UI.Lobby.LobbyClientState.reset();
        com.dod.unrealzaruba.Schematics.PurchaseClientState.reset();
        com.dod.unrealzaruba.UI.Vehicles.TeamVehiclesState.reset();
        com.dod.unrealzaruba.Schematics.PurchaseGhostRenderer.reset();
        MapPackClient.reset();
    }
}
