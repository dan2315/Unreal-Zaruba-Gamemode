package com.dod.unrealzaruba.NetworkPackets;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Thin adapter over {@link IPayloadContext} exposing the subset of Forge's NetworkEvent.Context the packets use.
 */
public class PacketContext {
    private final IPayloadContext context;

    public PacketContext(IPayloadContext context) {
        this.context = context;
    }

    public void enqueueWork(Runnable work) {
        context.enqueueWork(work);
    }

    /** The sending player for serverbound packets, otherwise null. */
    public ServerPlayer getSender() {
        return context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    /** No-op: NeoForge marks payloads handled automatically. */
    public void setPacketHandled(boolean handled) {
    }
}
