package com.dod.unrealzaruba.Gamemodes.Objectives.ProgressDisplay;

import com.dod.unrealzaruba.NetworkPackets.ClientboundUpdateTopScorePacket;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import static com.dod.unrealzaruba.WorldManager.WorldManager.server;

public class NetworkedTopHud implements IProgressDisplay {
    private final byte id;
    private final short requiredPoints;

    public NetworkedTopHud(byte id, short requiredPoints) {
        this.id = id;
        this.requiredPoints = requiredPoints;
    }

    @Override
    public void updateProgress(float progress) {
        for (var player :  server.getPlayerList().getPlayers()) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundUpdateTopScorePacket(id, (short) (requiredPoints * progress)));
        }
    }

    @Override
    public void updatePlayerVisibility(ServerPlayer player) {

    }

    @Override
    public void clear() {
        for (var player :  server.getPlayerList().getPlayers()) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundUpdateTopScorePacket(false));
        }
    }
}
