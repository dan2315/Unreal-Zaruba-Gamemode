package com.dod.unrealzaruba.NetworkPackets.Economy;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.Respawn.RespawnClientState;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** Where the unopened supply crate is, for the map waypoint; null when there is none. */
public class ClientboundSupplyDropPacket implements ModPacket {
    private final BlockPos pos;

    public ClientboundSupplyDropPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ClientboundSupplyDropPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.pos != null);
        if (msg.pos != null) buf.writeBlockPos(msg.pos);
    }

    public static ClientboundSupplyDropPacket decode(FriendlyByteBuf buf) {
        return new ClientboundSupplyDropPacket(buf.readBoolean() ? buf.readBlockPos() : null);
    }

    public static void handle(ClientboundSupplyDropPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> RespawnClientState.setSupplyDrop(msg.pos));
        ctx.get().setPacketHandled(true);
    }
}
