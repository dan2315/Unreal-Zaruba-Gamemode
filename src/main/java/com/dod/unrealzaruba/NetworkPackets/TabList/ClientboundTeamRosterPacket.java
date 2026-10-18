package com.dod.unrealzaruba.NetworkPackets.TabList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.TabList.TeamRosterState;

import net.minecraft.network.FriendlyByteBuf;

/** The players that are in a team and which one, for the player list on TAB. Players not listed are in no team. */
public class ClientboundTeamRosterPacket implements ModPacket {
    public record Entry(UUID player, byte team) {}

    private final List<Entry> entries;

    public ClientboundTeamRosterPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public static void encode(ClientboundTeamRosterPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entries.size());
        for (Entry entry : msg.entries) {
            buf.writeUUID(entry.player());
            buf.writeByte(entry.team());
        }
    }

    public static ClientboundTeamRosterPacket decode(FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), 512);
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(new Entry(buf.readUUID(), buf.readByte()));
        return new ClientboundTeamRosterPacket(entries);
    }

    public static void handle(ClientboundTeamRosterPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> TeamRosterState.set(msg.entries));
        ctx.get().setPacketHandled(true);
    }
}
