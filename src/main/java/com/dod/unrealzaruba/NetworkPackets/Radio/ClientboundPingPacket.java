package com.dod.unrealzaruba.NetworkPackets.Radio;

import java.util.UUID;
import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Radiopings.PingClientState;
import com.dod.unrealzaruba.Radiopings.PingType;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * A teammate's ping: who placed it, of which kind, the ground anchor and the diamond height, when it dies,
 * what it rides on (a Sable hull UUID, a client entity runtime id, or neither for a static mark).
 * Hull position updates for ridden pings come separately; entities are resolved live on the client.
 */
public class ClientboundPingPacket implements ModPacket {
    private final UUID sender;
    private final String senderName;
    private final byte typeId;
    private final Vec3 anchor;
    private final double diamondY;
    private final long expireAt;
    private final UUID vehicleId;
    private final int entityId;

    public ClientboundPingPacket(UUID sender, String senderName, byte typeId, Vec3 anchor, double diamondY,
            long expireAt, UUID vehicleId, int entityId) {
        this.sender = sender;
        this.senderName = senderName;
        this.typeId = typeId;
        this.anchor = anchor;
        this.diamondY = diamondY;
        this.expireAt = expireAt;
        this.vehicleId = vehicleId;
        this.entityId = entityId;
    }

    public static void encode(ClientboundPingPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.sender);
        buf.writeUtf(msg.senderName, 32);
        buf.writeByte(msg.typeId);
        buf.writeDouble(msg.anchor.x);
        buf.writeDouble(msg.anchor.y);
        buf.writeDouble(msg.anchor.z);
        buf.writeDouble(msg.diamondY);
        buf.writeLong(msg.expireAt);
        buf.writeBoolean(msg.vehicleId != null);
        if (msg.vehicleId != null) buf.writeUUID(msg.vehicleId);
        buf.writeInt(msg.entityId);
    }

    public static ClientboundPingPacket decode(FriendlyByteBuf buf) {
        UUID sender = buf.readUUID();
        String senderName = buf.readUtf(32);
        byte typeId = buf.readByte();
        Vec3 anchor = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double diamondY = buf.readDouble();
        long expireAt = buf.readLong();
        UUID vehicleId = buf.readBoolean() ? buf.readUUID() : null;
        int entityId = buf.readInt();
        return new ClientboundPingPacket(sender, senderName, typeId, anchor, diamondY, expireAt, vehicleId, entityId);
    }

    public static void handle(ClientboundPingPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> PingClientState.add(msg.sender, msg.senderName,
                PingType.byId(msg.typeId), msg.anchor, msg.diamondY, msg.expireAt, msg.vehicleId, msg.entityId));
        ctx.get().setPacketHandled(true);
    }
}
