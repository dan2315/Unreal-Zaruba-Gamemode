package com.dod.unrealzaruba.Feed;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

/** Kills and deaths of every player in the match, for the player list on TAB. It starts again with every match. */
public final class MatchStats {
    private static final Map<UUID, int[]> STATS = new HashMap<>();

    private MatchStats() {}

    /** A real death: the victim has died once more and the killer, if an enemy player, has killed once more. */
    public static void onDeath(ServerPlayer victim, ServerPlayer enemyKiller) {
        STATS.computeIfAbsent(victim.getUUID(), id -> new int[2])[1]++;
        if (enemyKiller != null) STATS.computeIfAbsent(enemyKiller.getUUID(), id -> new int[2])[0]++;
    }

    public static int kills(UUID player) {
        int[] stats = STATS.get(player);
        return stats == null ? 0 : stats[0];
    }

    public static int deaths(UUID player) {
        int[] stats = STATS.get(player);
        return stats == null ? 0 : stats[1];
    }

    public static void reset() {
        STATS.clear();
    }
}
