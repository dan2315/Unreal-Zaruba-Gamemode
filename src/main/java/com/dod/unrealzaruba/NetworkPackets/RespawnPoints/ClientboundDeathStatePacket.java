package com.dod.unrealzaruba.NetworkPackets.RespawnPoints;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.Respawn.ClientRespawnHandlers;

import net.minecraft.network.FriendlyByteBuf;

/** The player died (map opens to pick a respawn point) or respawned (map closes). */
public class ClientboundDeathStatePacket implements ModPacket {
    private final boolean dead;
    private final int respawnSeconds;
    /** Where the body lies and which way the player faced: the flyover rises over it. */
    private final double x, y, z;
    private final float yaw;

    public ClientboundDeathStatePacket(boolean dead, int respawnSeconds) {
        this(dead, respawnSeconds, 0, 0, 0, 0f);
    }

    public ClientboundDeathStatePacket(boolean dead, int respawnSeconds, double x, double y, double z, float yaw) {
        this.dead = dead;
        this.respawnSeconds = respawnSeconds;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
    }

    public static void encode(ClientboundDeathStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.dead);
        buf.writeVarInt(msg.respawnSeconds);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.yaw);
    }

    public static ClientboundDeathStatePacket decode(FriendlyByteBuf buf) {
        return new ClientboundDeathStatePacket(buf.readBoolean(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
    }

    public static void handle(ClientboundDeathStatePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            com.dod.unrealzaruba.UI.Respawn.DeathFlyover.setSpot(msg.x, msg.y, msg.z, msg.yaw);
            ClientRespawnHandlers.onDeathState(msg.dead, msg.respawnSeconds);
        });
        ctx.get().setPacketHandled(true);
    }
}
