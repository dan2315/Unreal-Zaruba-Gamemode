package com.dod.UnrealZaruba.Gamemodes.GameTimer;

import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.TimerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.network.PacketDistributor;

public class NetworkedTimer implements IGameTimer {

    @Override
    public void setup() {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(player -> {
            NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player), new TimerPacket(0, 0, true)
                );
        });
    }

    @Override
    public void update(int seconds, int minutes, boolean isVisible) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(player -> {
            NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player), new TimerPacket(seconds, minutes, true)
                );
        });
    }

    @Override
    public void reset() {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(player -> {
            NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player), new TimerPacket(0, 0, false)
                );
        });
    }
    
}
