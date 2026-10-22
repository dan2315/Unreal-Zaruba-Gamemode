package com.dod.unrealzaruba.NetworkPackets.Vehicles;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Vehicles.VehicleFlip;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Sent every tick while the key for turning an overturned vehicle is held. */
public class FlipVehiclePacket implements ModPacket {
    public static void encode(FlipVehiclePacket msg, FriendlyByteBuf buf) {}

    public static FlipVehiclePacket decode(FriendlyByteBuf buf) {
        return new FlipVehiclePacket();
    }

    public static void handle(FlipVehiclePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) VehicleFlip.held(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
