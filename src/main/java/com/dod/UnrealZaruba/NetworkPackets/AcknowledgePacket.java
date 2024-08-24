package com.dod.UnrealZaruba.NetworkPackets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.UUID;

public class AcknowledgePacket {
    private final int packetId;
    private final UUID reciever;

    public AcknowledgePacket(int packetId, UUID reciever) {
        this.packetId = packetId;
        this.reciever = reciever;
    }

    public static void encode(AcknowledgePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.packetId);
        buf.writeUUID(msg.reciever);
    }

    public static AcknowledgePacket decode(FriendlyByteBuf buf) {
        return new AcknowledgePacket(buf.readInt(), buf.readUUID());
    }

    public static void handle(AcknowledgePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle acknowledgment logic on server side
            NetworkHandler.markPacketAsAcknowledged(msg.packetId);
        });
        ctx.get().setPacketHandled(true);
    }
}
