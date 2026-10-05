package com.dod.unrealzaruba.NetworkPackets.MapPack;

import java.util.List;
import java.util.function.Supplier;

import com.dod.unrealzaruba.ModIntegrations.Xaero.MapPackClient;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;

/** The current map's pre-rendered world map: its hash and region files. The client asks for it if not cached. */
public class ClientboundMapPackOfferPacket implements ModPacket {
    public record FileInfo(String name, int size) {}

    private final String mapName;
    private final String hash;
    private final List<FileInfo> files;

    public ClientboundMapPackOfferPacket(String mapName, String hash, List<FileInfo> files) {
        this.mapName = mapName;
        this.hash = hash;
        this.files = files;
    }

    public static void encode(ClientboundMapPackOfferPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.mapName);
        buf.writeUtf(msg.hash);
        buf.writeCollection(msg.files, (b, file) -> {
            b.writeUtf(file.name());
            b.writeVarInt(file.size());
        });
    }

    public static ClientboundMapPackOfferPacket decode(FriendlyByteBuf buf) {
        return new ClientboundMapPackOfferPacket(buf.readUtf(), buf.readUtf(),
                buf.readList(b -> new FileInfo(b.readUtf(), b.readVarInt())));
    }

    public static void handle(ClientboundMapPackOfferPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> MapPackClient.onOffer(msg.mapName, msg.hash, msg.files));
        ctx.get().setPacketHandled(true);
    }
}
