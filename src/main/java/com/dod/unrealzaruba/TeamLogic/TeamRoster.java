package com.dod.unrealzaruba.TeamLogic;

import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Feed.Feed;
import com.dod.unrealzaruba.NetworkPackets.TabList.ClientboundTeamRosterPacket;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What the player list on TAB needs from the server: which team each player is in (two columns, one per team) and how the server
 * is doing (ticks per second and the time of a tick). The teams are sent when they change, checked once a second; the server load
 * every two seconds; everything to a player who connects.
 */
public final class TeamRoster {
    private static List<ClientboundTeamRosterPacket.Entry> last = List.of();
    private static int ticks;

    private TeamRoster() {}

    public static void tick(MinecraftServer server) {
        if (++ticks % 20 != 0) return;
        List<ClientboundTeamRosterPacket.Entry> now = current(server);
        boolean changed = !now.equals(last);
        if (!changed && ticks % 40 != 0) return;
        last = now;
        PacketDistributor.sendToAllPlayers(packet(server, now));
    }

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet(player.getServer(), current(player.getServer())));
    }

    private static ClientboundTeamRosterPacket packet(MinecraftServer server, List<ClientboundTeamRosterPacket.Entry> entries) {
        float mspt = server == null ? 0f : server.getAverageTickTimeNanos() / 1_000_000f;
        // A server cannot tick faster than 20 times a second; slower, the ticks per second follow from the time of a tick
        float tps = mspt <= 50f ? 20f : 1000f / mspt;
        return new ClientboundTeamRosterPacket(entries, tps, mspt);
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
