package com.dod.unrealzaruba.NetworkPackets;

import com.dod.unrealzaruba.UI.ScoreOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundUpdateTopScorePacket {
    boolean visible;
    byte id;
    short score;

    public ClientboundUpdateTopScorePacket(byte id, short score) {
        this(id, score, true);
    }

    public ClientboundUpdateTopScorePacket(boolean visible) {
        this((byte) 0,(short) 0, true);
    }

    public ClientboundUpdateTopScorePacket(byte id, short score, boolean visible) {
        this.visible = visible;
        this.id = id;
        this.score = score;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(visible);
        buffer.writeByte(id);
        buffer.writeShort(score);
    }

    public static ClientboundUpdateTopScorePacket decode(FriendlyByteBuf buffer) {
        boolean visible = buffer.readBoolean();
        return new ClientboundUpdateTopScorePacket(buffer.readByte(), buffer.readShort(), visible);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (visible) {
                ScoreOverlay.INSTANCE.SetScore(id, score);
                ScoreOverlay.INSTANCE.SetVisible(true);
            } else {
                ScoreOverlay.INSTANCE.SetVisible(false);
            }
        });
    }
}
