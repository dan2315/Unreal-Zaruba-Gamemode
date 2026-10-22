package com.dod.unrealzaruba.UI.Respawn;

import java.util.List;

import com.dod.unrealzaruba.ModIntegrations.WaypointManager;
import com.dod.unrealzaruba.NetworkPackets.SelectRespawnPointPacket;
import com.dod.unrealzaruba.UI.CustomDeathScreen;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client side of death and respawn. On death the world map opens so the player can pick a respawn point on it; without
 * Xaero's World Map the old list screen is used.
 */
public final class ClientRespawnHandlers {
    private ClientRespawnHandlers() {}

    public static void onDeathState(boolean dead, int seconds) {
        RespawnClientState.setDead(dead, seconds);
        Minecraft mc = Minecraft.getInstance();
        if (dead) {
            // While the camera flies over the body nothing opens; the map comes when the waiting is over
            if (!RespawnClientState.isChoosing()) return;
            if (!WaypointManager.INSTANCE.openMapForRespawn()) {
                List<CustomDeathScreen.RespawnUiElement> points = RespawnClientState.view().points().stream()
                        .map(point -> new CustomDeathScreen.RespawnUiElement(point.id(), point.name()))
                        .toList();
                mc.setScreen(new CustomDeathScreen(points));
            }
        } else {
            WaypointManager.INSTANCE.closeMapAfterRespawn();
            if (mc.screen instanceof CustomDeathScreen) {
                mc.setScreen(null);
            }
        }
    }

    public static void onTimer(int seconds) {
        RespawnClientState.setSecondsLeft(seconds);
        if (Minecraft.getInstance().screen instanceof CustomDeathScreen screen) {
            screen.updateRespawnTimer(seconds);
        }
    }

    public static void select(byte respawnPointId) {
        PacketDistributor.sendToServer(new SelectRespawnPointPacket(respawnPointId));
    }
}
