package com.dod.unrealzaruba.NetworkPackets.TabList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.TabList.TeamRosterState;

import net.minecraft.network.FriendlyByteBuf;

/**
 * What the player list on TAB shows from the server: the players that are in a team and which one (players not listed are in no team),
 * and the load of the server, as ticks per second and the time of a tick in milliseconds.
 */
public class ClientboundTeamRosterPacket implements ModPacket {
    public record Entry(UUID player, byte team, short kills, short deaths) {}

    private final List<Entry> entries;
    private final float tps;
    private final float mspt;

    public ClientboundTeamRosterPacket(List<Entry> entries, float tps, float mspt) {
        this.entries = entries;
        this.tps = tps;
        this.mspt = mspt;
    }

    public static void encode(ClientboundTeamRosterPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.tps);
        buf.writeFloat(msg.mspt);
        buf.writeVarInt(msg.entries.size());
        for (Entry entry : msg.entries) {
            buf.writeUUID(entry.player());
            buf.writeByte(entry.team());
            buf.writeShort(entry.kills());
            buf.writeShort(entry.deaths());
        }
    }

    public static ClientboundTeamRosterPacket decode(FriendlyByteBuf buf) {
        float tps = buf.readFloat();
        float mspt = buf.readFloat();
        int count = Math.min(buf.readVarInt(), 512);
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(new Entry(buf.readUUID(), buf.readByte(), buf.readShort(), buf.readShort()));
        return new ClientboundTeamRosterPacket(entries, tps, mspt);
    }

    public static void handle(ClientboundTeamRosterPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> TeamRosterState.set(msg.entries, msg.tps, msg.mspt));
        ctx.get().setPacketHandled(true);
    }
}
