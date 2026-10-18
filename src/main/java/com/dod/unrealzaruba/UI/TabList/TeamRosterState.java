package com.dod.unrealzaruba.UI.TabList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.NetworkPackets.TabList.ClientboundTeamRosterPacket;

/** The teams of the players, as the server last said. */
public final class TeamRosterState {
    private static volatile Map<UUID, TeamColor> teams = Map.of();

    private TeamRosterState() {}

    public static void set(List<ClientboundTeamRosterPacket.Entry> entries) {
        Map<UUID, TeamColor> map = new HashMap<>();
        TeamColor[] colors = TeamColor.values();
        for (ClientboundTeamRosterPacket.Entry entry : entries) {
            if (entry.team() >= 0 && entry.team() < colors.length) map.put(entry.player(), colors[entry.team()]);
        }
        teams = map;
    }

    /** The team of a player, or null for none. */
    public static TeamColor teamOf(UUID player) {
        return teams.get(player);
    }

    public static boolean any() {
        return !teams.isEmpty();
    }

    public static void reset() {
        teams = Map.of();
    }
}
