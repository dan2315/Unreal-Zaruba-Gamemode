package com.dod.unrealzaruba.NetworkPackets.MapPack;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.WorldManager.MapPacks;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** The client doesn't have the offered map pack and wants it sent. */
public class RequestMapPackPacket implements ModPacket {
    private final String hash;

    public RequestMapPackPacket(String hash) {
        this.hash = hash;
    }

    public static void encode(RequestMapPackPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.hash);
    }

    public static RequestMapPackPacket decode(FriendlyByteBuf buf) {
        return new RequestMapPackPacket(buf.readUtf(64));
    }

    public static void handle(RequestMapPackPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MapPacks.request(player, msg.hash);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
