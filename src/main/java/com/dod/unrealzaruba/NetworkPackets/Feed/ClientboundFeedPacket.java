package com.dod.unrealzaruba.NetworkPackets.Feed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.dod.unrealzaruba.Feed.FeedKind;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.Feed.FeedState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * One line of the event feed. For a kill: the names and team colours, the weapon (an item stack as NBT, read with the
 * registries of the client), the reward and bounty, the streak. For other events: a translation key and its arguments, and the
 * colour of the team the event is about (in {@code colorA}).
 */
public class ClientboundFeedPacket implements ModPacket {
    private final FeedKind kind;
    private final int colorA;
    private final int colorB;
    private final String nameA;
    private final String nameB;
    private final CompoundTag icon;
    private final int reward;
    private final int bounty;
    private final int streak;
    private final String key;
    private final List<String> args;

    public ClientboundFeedPacket(FeedKind kind, int colorA, int colorB, String nameA, String nameB, CompoundTag icon,
                                 int reward, int bounty, int streak, String key, List<String> args) {
        this.kind = kind;
        this.colorA = colorA;
        this.colorB = colorB;
        this.nameA = nameA;
        this.nameB = nameB;
        this.icon = icon;
        this.reward = reward;
        this.bounty = bounty;
        this.streak = streak;
        this.key = key;
        this.args = args;
    }

    public static void encode(ClientboundFeedPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.kind.ordinal());
        buf.writeInt(msg.colorA);
        buf.writeInt(msg.colorB);
        buf.writeUtf(msg.nameA, 64);
        buf.writeUtf(msg.nameB, 64);
        buf.writeBoolean(msg.icon != null);
        if (msg.icon != null) buf.writeNbt(msg.icon);
        buf.writeVarInt(msg.reward);
        buf.writeVarInt(msg.bounty);
        buf.writeVarInt(msg.streak);
        buf.writeUtf(msg.key, 128);
        buf.writeVarInt(msg.args.size());
        for (String arg : msg.args) buf.writeUtf(arg, 160);
    }

    public static ClientboundFeedPacket decode(FriendlyByteBuf buf) {
        FeedKind kind = FeedKind.values()[Math.max(0, Math.min(FeedKind.values().length - 1, buf.readByte()))];
        int colorA = buf.readInt();
        int colorB = buf.readInt();
        String nameA = buf.readUtf(64);
        String nameB = buf.readUtf(64);
        CompoundTag icon = buf.readBoolean() ? buf.readNbt() : null;
        int reward = buf.readVarInt();
        int bounty = buf.readVarInt();
        int streak = buf.readVarInt();
        String key = buf.readUtf(128);
        int count = Math.min(buf.readVarInt(), 8);
        List<String> args = new ArrayList<>(count);
        for (int i = 0; i < count; i++) args.add(buf.readUtf(160));
        return new ClientboundFeedPacket(kind, colorA, colorB, nameA, nameB, icon, reward, bounty, streak, key, args);
    }

    public static void handle(ClientboundFeedPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> FeedState.add(msg.kind, msg.colorA, msg.colorB, msg.nameA, msg.nameB, msg.icon,
                msg.reward, msg.bounty, msg.streak, msg.key, msg.args));
        ctx.get().setPacketHandled(true);
    }
}
