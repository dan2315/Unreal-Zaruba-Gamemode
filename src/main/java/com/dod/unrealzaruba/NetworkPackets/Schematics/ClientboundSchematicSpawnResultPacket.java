package com.dod.unrealzaruba.NetworkPackets.Schematics;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Schematics.ClientSchematicHandlers;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/** Whether the vehicle was spawned, with the reason when it wasn't. */
public class ClientboundSchematicSpawnResultPacket implements ModPacket {
    public final boolean success;
    public final Component message;

    public ClientboundSchematicSpawnResultPacket(boolean success, Component message) {
        this.success = success;
        this.message = message;
    }

    public static void encode(ClientboundSchematicSpawnResultPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.success);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, msg.message);
    }

    public static ClientboundSchematicSpawnResultPacket decode(FriendlyByteBuf buf) {
        return new ClientboundSchematicSpawnResultPacket(buf.readBoolean(),
                ComponentSerialization.TRUSTED_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf));
    }

    public static void handle(ClientboundSchematicSpawnResultPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> ClientSchematicHandlers.onSpawnResult(msg.success, msg.message));
        ctx.get().setPacketHandled(true);
    }
}
