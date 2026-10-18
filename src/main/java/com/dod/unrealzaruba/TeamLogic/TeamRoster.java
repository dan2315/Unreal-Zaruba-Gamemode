package com.dod.unrealzaruba.TeamLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Feed.Feed;
import com.dod.unrealzaruba.NetworkPackets.TabList.ClientboundTeamRosterPacket;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Which team each player is in, for the player list the clients draw on TAB (two columns, one per team). It is sent when it
 * changes, checked once a second, and to a player who connects.
 */
public final class TeamRoster {
    private static List<ClientboundTeamRosterPacket.Entry> last = List.of();
    private static int ticks;

    private TeamRoster() {}

    public static void tick(MinecraftServer server) {
        if (++ticks % 20 != 0) return;
        List<ClientboundTeamRosterPacket.Entry> now = current(server);
        if (now.equals(last)) return;
        last = now;
        PacketDistributor.sendToAllPlayers(new ClientboundTeamRosterPacket(now));
    }

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundTeamRosterPacket(current(player.getServer())));
    }

    private static List<ClientboundTeamRosterPacket.Entry> current(MinecraftServer server) {
        List<ClientboundTeamRosterPacket.Entry> entries = new ArrayList<>();
        if (server == null) return entries;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TeamColor team = Feed.teamOf(player);
            if (team != null) entries.add(new ClientboundTeamRosterPacket.Entry(player.getUUID(), (byte) team.ordinal()));
        }
        return entries;
    }

    /** Back to nothing (a new match). */
    public static void reset() {
        last = List.of();
    }
}
