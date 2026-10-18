package com.dod.unrealzaruba.UI.TabList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.NetworkPackets.TabList.ClientboundTeamRosterPacket;

/** The teams of the players and the load of the server, as the server last said. */
public final class TeamRosterState {
    private static volatile Map<UUID, TeamColor> teams = Map.of();
    private static volatile Map<UUID, int[]> stats = Map.of();
    private static volatile float tps = -1f;
    private static volatile float mspt;

    private TeamRosterState() {}

    public static void set(List<ClientboundTeamRosterPacket.Entry> entries, float serverTps, float serverMspt) {
        Map<UUID, TeamColor> map = new HashMap<>();
        Map<UUID, int[]> counts = new HashMap<>();
        TeamColor[] colors = TeamColor.values();
        for (ClientboundTeamRosterPacket.Entry entry : entries) {
            if (entry.team() >= 0 && entry.team() < colors.length) map.put(entry.player(), colors[entry.team()]);
            counts.put(entry.player(), new int[]{entry.kills(), entry.deaths()});
        }
        teams = map;
        stats = counts;
        tps = serverTps;
        mspt = serverMspt;
    }

    /** The team of a player, or null for none. */
    public static TeamColor teamOf(UUID player) {
        return teams.get(player);
    }

    /** Kills of a player in this match, or -1 when the server does not count them (no team). */
    public static int kills(UUID player) {
        int[] counts = stats.get(player);
        return counts == null ? -1 : counts[0];
    }

    public static int deaths(UUID player) {
        int[] counts = stats.get(player);
        return counts == null ? -1 : counts[1];
    }

    public static boolean any() {
        return !teams.isEmpty();
    }

    /** Ticks per second of the server, or a negative number when it has not said yet. */
    public static float tps() {
        return tps;
    }

    /** Milliseconds one tick of the server takes. */
    public static float mspt() {
        return mspt;
    }

    public static void reset() {
        teams = Map.of();
        stats = Map.of();
        tps = -1f;
        mspt = 0f;
    }
}
