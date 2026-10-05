package com.dod.unrealzaruba.NetworkPackets;

import com.dod.unrealzaruba.UI.ScoreOverlay;
import net.minecraft.network.FriendlyByteBuf;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import java.util.function.Supplier;

public class ClientboundUpdateTopScorePacket implements ModPacket {
    boolean visible;
    byte id;
    short score;
    /** Points needed to win, for the score bar. */
    short goal;

    public ClientboundUpdateTopScorePacket(byte id, short score, short goal) {
        this(id, score, goal, true);
    }

    public ClientboundUpdateTopScorePacket(boolean visible) {
        this((byte) 0, (short) 0, (short) 0, visible);
    }

    public ClientboundUpdateTopScorePacket(byte id, short score, short goal, boolean visible) {
        this.visible = visible;
        this.id = id;
        this.score = score;
        this.goal = goal;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(visible);
        buffer.writeByte(id);
        buffer.writeShort(score);
        buffer.writeShort(goal);
    }

    public static ClientboundUpdateTopScorePacket decode(FriendlyByteBuf buffer) {
        boolean visible = buffer.readBoolean();
        return new ClientboundUpdateTopScorePacket(buffer.readByte(), buffer.readShort(), buffer.readShort(), visible);
    }

    public void handle(Supplier<PacketContext> context) {
        context.get().enqueueWork(() -> {
            if (visible) {
                ScoreOverlay.INSTANCE.SetScore(id, score, goal);
                ScoreOverlay.INSTANCE.SetVisible(true);
            } else {
                ScoreOverlay.INSTANCE.SetVisible(false);
            }
        });
    }
}
