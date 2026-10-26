package com.dod.unrealzaruba.NetworkPackets.Radio;

import java.util.UUID;
import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Radiopings.RadioClient;

import net.minecraft.network.FriendlyByteBuf;

/** A teammate's radio phrase: who said it, which one, and the sender's team colour for the chat line. */
public class ClientboundRadioPacket implements ModPacket {
    private final UUID sender;
    private final String senderName;
    private final byte phraseId;
    private final int color;

    public ClientboundRadioPacket(UUID sender, String senderName, byte phraseId, int color) {
        this.sender = sender;
        this.senderName = senderName;
        this.phraseId = phraseId;
        this.color = color;
    }

    public static void encode(ClientboundRadioPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.sender);
        buf.writeUtf(msg.senderName, 32);
        buf.writeByte(msg.phraseId);
        buf.writeInt(msg.color);
    }

    public static ClientboundRadioPacket decode(FriendlyByteBuf buf) {
        return new ClientboundRadioPacket(buf.readUUID(), buf.readUtf(32), buf.readByte(), buf.readInt());
    }

    public static void handle(ClientboundRadioPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> RadioClient.onRadio(msg.sender, msg.senderName, msg.phraseId, msg.color));
        ctx.get().setPacketHandled(true);
    }
}
