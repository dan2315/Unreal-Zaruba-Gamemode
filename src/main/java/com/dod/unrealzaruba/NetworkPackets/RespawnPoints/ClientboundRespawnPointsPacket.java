package com.dod.unrealzaruba.NetworkPackets.RespawnPoints;

import java.util.function.Supplier;

import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsView;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.Respawn.RespawnClientState;

import net.minecraft.network.FriendlyByteBuf;

/** The player's team respawn points and which one they'll use. */
public class ClientboundRespawnPointsPacket implements ModPacket {
    private final RespawnPointsView view;

    public ClientboundRespawnPointsPacket(RespawnPointsView view) {
        this.view = view;
    }

    public static void encode(ClientboundRespawnPointsPacket msg, FriendlyByteBuf buf) {
        msg.view.encode(buf);
    }

    public static ClientboundRespawnPointsPacket decode(FriendlyByteBuf buf) {
        return new ClientboundRespawnPointsPacket(RespawnPointsView.decode(buf));
    }

    public static void handle(ClientboundRespawnPointsPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> RespawnClientState.setView(msg.view));
        ctx.get().setPacketHandled(true);
    }
}
