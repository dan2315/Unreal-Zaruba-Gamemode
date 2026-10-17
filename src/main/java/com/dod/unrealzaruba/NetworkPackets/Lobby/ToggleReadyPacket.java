package com.dod.unrealzaruba.NetworkPackets.Lobby;

import java.util.function.Supplier;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.Lobby.LobbyService;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** The ready button of the lobby screen: the same as typing /ready. */
public class ToggleReadyPacket implements ModPacket {
    public static void encode(ToggleReadyPacket msg, FriendlyByteBuf buf) {}

    public static ToggleReadyPacket decode(FriendlyByteBuf buf) {
        return new ToggleReadyPacket();
    }

    public static void handle(ToggleReadyPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && GamemodeManager.instance != null && GamemodeManager.instance.IsVoting()) {
                LobbyService.toggleReady(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
