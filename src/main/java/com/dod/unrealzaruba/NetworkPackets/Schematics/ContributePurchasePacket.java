package com.dod.unrealzaruba.NetworkPackets.Schematics;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Vehicles.PurchaseSessions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Add money to the vehicle being bought at a purchase block. The server checks the player, the range and the amount. */
public class ContributePurchasePacket implements ModPacket {
    public final BlockPos pos;
    public final int amount;

    public ContributePurchasePacket(BlockPos pos, int amount) {
        this.pos = pos;
        this.amount = amount;
    }

    public static void encode(ContributePurchasePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.amount);
    }

    public static ContributePurchasePacket decode(FriendlyByteBuf buf) {
        return new ContributePurchasePacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(ContributePurchasePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) PurchaseSessions.contribute(player, msg.pos, msg.amount);
        });
        ctx.get().setPacketHandled(true);
    }
}
