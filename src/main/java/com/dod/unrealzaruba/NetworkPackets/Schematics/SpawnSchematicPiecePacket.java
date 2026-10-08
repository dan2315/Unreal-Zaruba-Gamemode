package com.dod.unrealzaruba.NetworkPackets.Schematics;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Vehicles.SchematicSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * A piece of the blueprint a player wants spawned at a vehicle purchase block. The client downloads it from the
 * backend (the server has no player token) and sends it in pieces: serverbound payloads are limited to 32 KB.
 */
public class SpawnSchematicPiecePacket implements ModPacket {
    public static final int MAX_PIECE = 24 * 1024;

    public final BlockPos pos;
    public final long schematicId;
    public final String name;
    public final int totalSize;
    public final int offset;
    public final byte[] data;

    public SpawnSchematicPiecePacket(BlockPos pos, long schematicId, String name, int totalSize, int offset, byte[] data) {
        this.pos = pos;
        this.schematicId = schematicId;
        this.name = name;
        this.totalSize = totalSize;
        this.offset = offset;
        this.data = data;
    }

    public static void encode(SpawnSchematicPiecePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarLong(msg.schematicId);
        buf.writeUtf(msg.name, 64);
        buf.writeVarInt(msg.totalSize);
        buf.writeVarInt(msg.offset);
        buf.writeByteArray(msg.data);
    }

    public static SpawnSchematicPiecePacket decode(FriendlyByteBuf buf) {
        return new SpawnSchematicPiecePacket(buf.readBlockPos(), buf.readVarLong(), buf.readUtf(64), buf.readVarInt(),
                buf.readVarInt(), buf.readByteArray(MAX_PIECE));
    }

    public static void handle(SpawnSchematicPiecePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                SchematicSpawner.onPiece(player, msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
