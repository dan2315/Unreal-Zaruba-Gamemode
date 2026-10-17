package com.dod.unrealzaruba.NetworkPackets.Schematics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Schematics.PurchaseClientHandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * How a joint vehicle purchase at a block stands: what is bought, the price, how much is paid, the seconds left, where
 * the vehicle will appear (for the ghost) and who paid what. {@code active} false means it ended, {@code completed}
 * says whether the vehicle was bought.
 */
public class ClientboundPurchaseSessionPacket implements ModPacket {
    public final BlockPos pos;
    public final boolean active;
    public final boolean completed;
    public final long schematicId;
    public final String name;
    public final int price;
    public final int paid;
    public final int ticksLeft;
    public final double centerX;
    public final double centerZ;
    public final double minimumBlockCenterY;
    public final String initiator;
    public final Map<String, Integer> contributors;

    public ClientboundPurchaseSessionPacket(BlockPos pos, boolean active, boolean completed, long schematicId, String name,
                                            int price, int paid, int ticksLeft, double centerX, double centerZ,
                                            double minimumBlockCenterY, String initiator, Map<String, Integer> contributors) {
        this.pos = pos;
        this.active = active;
        this.completed = completed;
        this.schematicId = schematicId;
        this.name = name;
        this.price = price;
        this.paid = paid;
        this.ticksLeft = ticksLeft;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.minimumBlockCenterY = minimumBlockCenterY;
        this.initiator = initiator;
        this.contributors = contributors;
    }

    public static void encode(ClientboundPurchaseSessionPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.completed);
        buf.writeVarLong(msg.schematicId);
        buf.writeUtf(msg.name, 64);
        buf.writeVarInt(msg.price);
        buf.writeVarInt(msg.paid);
        buf.writeVarInt(msg.ticksLeft);
        buf.writeDouble(msg.centerX);
        buf.writeDouble(msg.centerZ);
        buf.writeDouble(msg.minimumBlockCenterY);
        buf.writeUtf(msg.initiator, 32);
        buf.writeMap(msg.contributors, (b, key) -> b.writeUtf(key, 32), FriendlyByteBuf::writeVarInt);
    }

    public static ClientboundPurchaseSessionPacket decode(FriendlyByteBuf buf) {
        return new ClientboundPurchaseSessionPacket(buf.readBlockPos(), buf.readBoolean(), buf.readBoolean(), buf.readVarLong(),
                buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readUtf(32), buf.readMap(LinkedHashMap::new, b -> b.readUtf(32), FriendlyByteBuf::readVarInt));
    }

    public static void handle(ClientboundPurchaseSessionPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> PurchaseClientHandlers.onSession(msg));
        ctx.get().setPacketHandled(true);
    }
}
