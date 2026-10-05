package com.dod.unrealzaruba.NetworkPackets.MapPack;

import java.util.function.Supplier;

import com.dod.unrealzaruba.ModIntegrations.Xaero.MapPackClient;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;

/** A piece of one region file of a map pack. */
public class ClientboundMapPackDataPacket implements ModPacket {
    private final String hash;
    private final int fileIndex;
    private final int offset;
    private final byte[] data;

    public ClientboundMapPackDataPacket(String hash, int fileIndex, int offset, byte[] data) {
        this.hash = hash;
        this.fileIndex = fileIndex;
        this.offset = offset;
        this.data = data;
    }

    public static void encode(ClientboundMapPackDataPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.hash);
        buf.writeVarInt(msg.fileIndex);
        buf.writeVarInt(msg.offset);
        buf.writeByteArray(msg.data);
    }

    public static ClientboundMapPackDataPacket decode(FriendlyByteBuf buf) {
        return new ClientboundMapPackDataPacket(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(256 * 1024));
    }

    public static void handle(ClientboundMapPackDataPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> MapPackClient.onData(msg.hash, msg.fileIndex, msg.offset, msg.data));
        ctx.get().setPacketHandled(true);
    }
}
