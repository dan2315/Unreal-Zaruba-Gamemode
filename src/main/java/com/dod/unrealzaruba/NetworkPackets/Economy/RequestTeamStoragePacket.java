package com.dod.unrealzaruba.NetworkPackets.Economy;

import java.util.function.Supplier;

import com.dod.unrealzaruba.Economy.TeamStorage;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Team storage key pressed; the server opens it if the player is in their team's respawn zone. */
public class RequestTeamStoragePacket implements ModPacket {
    public static void encode(RequestTeamStoragePacket msg, FriendlyByteBuf buf) {
    }

    public static RequestTeamStoragePacket decode(FriendlyByteBuf buf) {
        return new RequestTeamStoragePacket();
    }

    public static void handle(RequestTeamStoragePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TeamStorage.open(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
