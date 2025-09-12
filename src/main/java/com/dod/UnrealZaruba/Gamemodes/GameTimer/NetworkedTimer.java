package com.dod.UnrealZaruba.Gamemodes.GameTimer;

import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.StartHudTimerPacket;
import com.dod.UnrealZaruba.NetworkPackets.StopHudTimerPacket;
import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.network.PacketDistributor;

public class NetworkedTimer implements IGameTimer {
    @Override
    public void startCountDown(long startTime, int durationSeconds) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(player -> {
            UnrealZaruba.LOGGER.warn("SENDING TIMER START PACKET TO " + player.getName());
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player), new StartHudTimerPacket(startTime, durationSeconds)
            );
        });
    }

    @Override
    public void stop() {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(player -> {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player), new StopHudTimerPacket()
            );
        });
    }
}
