package com.dod.unrealzaruba.NetworkPackets.Radio;

import java.util.UUID;
import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Radiopings.PingClientState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/** Live position of a vehicle a ping rides on: ground anchor under the hull and diamond height over its top. */
public class ClientboundPingVehiclePacket implements ModPacket {
    private final UUID vehicleId;
    private final Vec3 anchor;
    private final double diamondY;

    public ClientboundPingVehiclePacket(UUID vehicleId, Vec3 anchor, double diamondY) {
        this.vehicleId = vehicleId;
        this.anchor = anchor;
        this.diamondY = diamondY;
    }

    public static void encode(ClientboundPingVehiclePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.vehicleId);
        buf.writeDouble(msg.anchor.x);
        buf.writeDouble(msg.anchor.y);
        buf.writeDouble(msg.anchor.z);
        buf.writeDouble(msg.diamondY);
    }

    public static ClientboundPingVehiclePacket decode(FriendlyByteBuf buf) {
        return new ClientboundPingVehiclePacket(buf.readUUID(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readDouble());
    }

    public static void handle(ClientboundPingVehiclePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> PingClientState.updateVehicle(msg.vehicleId, msg.anchor, msg.diamondY));
        ctx.get().setPacketHandled(true);
    }
}
