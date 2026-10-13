package com.dod.unrealzaruba.NetworkPackets.Economy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.dod.unrealzaruba.Economy.SupplyCaseClientState;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Every supply case that is being carried or lies on the ground, for the light beams the clients draw over them.
 * The position is the entity's last known one; clients that have the entity loaded follow it smoothly instead.
 */
public class ClientboundSupplyCasePacket implements ModPacket {
    public record Entry(int entityId, double x, double y, double z, int color) {}

    private final List<Entry> entries;

    public ClientboundSupplyCasePacket(List<Entry> entries) {
        this.entries = entries;
    }

    public static void encode(ClientboundSupplyCasePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entries.size());
        for (Entry entry : msg.entries) {
            buf.writeVarInt(entry.entityId());
            buf.writeDouble(entry.x());
            buf.writeDouble(entry.y());
            buf.writeDouble(entry.z());
            buf.writeInt(entry.color());
        }
    }

    public static ClientboundSupplyCasePacket decode(FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), 256);
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt()));
        }
        return new ClientboundSupplyCasePacket(entries);
    }

    public static void handle(ClientboundSupplyCasePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> SupplyCaseClientState.set(msg.entries));
        ctx.get().setPacketHandled(true);
    }
}
