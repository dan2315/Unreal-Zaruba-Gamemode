package com.dod.unrealzaruba.NetworkPackets.Schematics;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Schematics.PurchaseClientHandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** Open the screen of the purchase going on at this block (the state packet comes just before it). */
public class ClientboundOpenPurchaseSessionPacket implements ModPacket {
    public final BlockPos pos;

    public ClientboundOpenPurchaseSessionPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ClientboundOpenPurchaseSessionPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static ClientboundOpenPurchaseSessionPacket decode(FriendlyByteBuf buf) {
        return new ClientboundOpenPurchaseSessionPacket(buf.readBlockPos());
    }

    public static void handle(ClientboundOpenPurchaseSessionPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> PurchaseClientHandlers.openSession(msg.pos));
        ctx.get().setPacketHandled(true);
    }
}
