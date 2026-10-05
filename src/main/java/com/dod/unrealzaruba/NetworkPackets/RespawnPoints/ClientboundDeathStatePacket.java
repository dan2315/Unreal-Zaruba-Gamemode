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

    public ClientboundDeathStatePacket(boolean dead, int respawnSeconds) {
        this.dead = dead;
        this.respawnSeconds = respawnSeconds;
    }

    public static void encode(ClientboundDeathStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.dead);
        buf.writeVarInt(msg.respawnSeconds);
    }

    public static ClientboundDeathStatePacket decode(FriendlyByteBuf buf) {
        return new ClientboundDeathStatePacket(buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(ClientboundDeathStatePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> ClientRespawnHandlers.onDeathState(msg.dead, msg.respawnSeconds));
        ctx.get().setPacketHandled(true);
    }
}
