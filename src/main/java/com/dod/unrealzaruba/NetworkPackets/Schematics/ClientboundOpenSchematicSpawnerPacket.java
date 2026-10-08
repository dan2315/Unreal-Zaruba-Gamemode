package com.dod.unrealzaruba.NetworkPackets.Schematics;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Schematics.ClientSchematicHandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** A player used a vehicle purchase block: open the schematics browser with a Spawn button for it. */
public class ClientboundOpenSchematicSpawnerPacket implements ModPacket {
    public final BlockPos pos;
    public final int price;
    public final int cooldownSeconds;

    public ClientboundOpenSchematicSpawnerPacket(BlockPos pos, int price, int cooldownSeconds) {
        this.pos = pos;
        this.price = price;
        this.cooldownSeconds = cooldownSeconds;
    }

    public static void encode(ClientboundOpenSchematicSpawnerPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.price);
        buf.writeVarInt(msg.cooldownSeconds);
    }

    public static ClientboundOpenSchematicSpawnerPacket decode(FriendlyByteBuf buf) {
        return new ClientboundOpenSchematicSpawnerPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ClientboundOpenSchematicSpawnerPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> ClientSchematicHandlers.openSpawner(msg.pos, msg.price, msg.cooldownSeconds));
        ctx.get().setPacketHandled(true);
    }
}
