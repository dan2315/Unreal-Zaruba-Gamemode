package com.dod.unrealzaruba.NetworkPackets.MapPack;

import java.util.function.Supplier;

import com.dod.unrealzaruba.ModIntegrations.Xaero.MapPackClient;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;

/** End of /mapscan: the client should upload its rendered game dimension map as this map's pack. */
public class ClientboundMapPackUploadRequestPacket implements ModPacket {
    private final String mapName;

    public ClientboundMapPackUploadRequestPacket(String mapName) {
        this.mapName = mapName;
    }

    public static void encode(ClientboundMapPackUploadRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.mapName);
    }

    public static ClientboundMapPackUploadRequestPacket decode(FriendlyByteBuf buf) {
        return new ClientboundMapPackUploadRequestPacket(buf.readUtf());
    }

    public static void handle(ClientboundMapPackUploadRequestPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> MapPackClient.startUpload(msg.mapName));
        ctx.get().setPacketHandled(true);
    }
}
