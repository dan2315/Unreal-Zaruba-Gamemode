package com.dod.unrealzaruba.NetworkPackets.MapPack;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.WorldManager.MapPacks;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** A piece of a region file an op uploads after /mapscan. Kept small: serverbound payloads are limited to 32 KB. */
public class UploadMapPackPacket implements ModPacket {
    public static final int MAX_PIECE = 24 * 1024;

    public final String mapName;
    public final int fileCount;
    public final String fileName;
    public final int fileSize;
    public final int offset;
    public final byte[] data;

    public UploadMapPackPacket(String mapName, int fileCount, String fileName, int fileSize, int offset, byte[] data) {
        this.mapName = mapName;
        this.fileCount = fileCount;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.offset = offset;
        this.data = data;
    }

    public static void encode(UploadMapPackPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.mapName);
        buf.writeVarInt(msg.fileCount);
        buf.writeUtf(msg.fileName);
        buf.writeVarInt(msg.fileSize);
        buf.writeVarInt(msg.offset);
        buf.writeByteArray(msg.data);
    }

    public static UploadMapPackPacket decode(FriendlyByteBuf buf) {
        return new UploadMapPackPacket(buf.readUtf(128), buf.readVarInt(), buf.readUtf(64), buf.readVarInt(),
                buf.readVarInt(), buf.readByteArray(MAX_PIECE));
    }

    public static void handle(UploadMapPackPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MapPacks.onUpload(player, msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
