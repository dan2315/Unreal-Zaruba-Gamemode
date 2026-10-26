package com.dod.unrealzaruba.NetworkPackets.Radio;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Radiopings.RadioService;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** A radio phrase chosen in the radio menu. The server checks it and reads it to the team. */
public class ServerboundRadioPacket implements ModPacket {
    private final byte phraseId;

    public ServerboundRadioPacket(byte phraseId) {
        this.phraseId = phraseId;
    }

    public static void encode(ServerboundRadioPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.phraseId);
    }

    public static ServerboundRadioPacket decode(FriendlyByteBuf buf) {
        return new ServerboundRadioPacket(buf.readByte());
    }

    public static void handle(ServerboundRadioPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                RadioService.onRadio(player, msg.phraseId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
