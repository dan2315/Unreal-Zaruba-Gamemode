package com.dod.unrealzaruba.Gamemodes.GameTimer;

import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.NetworkPackets.StartHudTimerPacket;
import com.dod.unrealzaruba.NetworkPackets.StopHudTimerPacket;
import com.dod.unrealzaruba.UnrealZaruba;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.network.PacketDistributor;

public class NetworkedTimer implements IGameTimer {
    @Override
    public void startCountDown(long startTime, int durationSeconds) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(player -> {
            UnrealZaruba.LOGGER.warn("SENDING TIMER START PACKET TO " + player.getName());
            PacketDistributor.sendToPlayer(player, new StartHudTimerPacket(startTime, durationSeconds)
            );
        });
    }

    @Override
    public void stop() {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(player -> {
            PacketDistributor.sendToPlayer(player, new StopHudTimerPacket()
            );
        });
    }
}
