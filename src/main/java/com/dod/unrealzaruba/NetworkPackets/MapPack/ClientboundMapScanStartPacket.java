package com.dod.unrealzaruba.NetworkPackets.MapPack;

import java.util.function.Supplier;

import com.dod.unrealzaruba.ModIntegrations.Xaero.MapPackClient;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;

/** Start of /mapscan: the client renders the game dimension into a new empty map, which it uploads at the end. */
public class ClientboundMapScanStartPacket implements ModPacket {
    public ClientboundMapScanStartPacket() {}

    public static void encode(ClientboundMapScanStartPacket msg, FriendlyByteBuf buf) {}

    public static ClientboundMapScanStartPacket decode(FriendlyByteBuf buf) {
        return new ClientboundMapScanStartPacket();
    }

    public static void handle(ClientboundMapScanStartPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(MapPackClient::startScan);
        ctx.get().setPacketHandled(true);
    }
}
