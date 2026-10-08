package com.dod.unrealzaruba.Economy;

import com.dod.unrealzaruba.Gamemodes.RespawnPoints.CapturePointRespawnPoint;
import com.dod.unrealzaruba.Gamemodes.RespawnPoints.IRespawnPoint;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.WorldManager.WorldManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * A team's safe areas: around each of its respawn points (base, tents, vehicles) and inside the capture areas it
 * holds. The buy menu and team storage only work there.
 */
public final class TeamZones {
    // Capture areas are drawn tight; let players stand at the edge
    private static final double CAPTURE_AREA_MARGIN = 2;

    private TeamZones() {}

    public static TeamContext teamOf(ServerPlayer player) {
        return PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext context ? context.Team() : null;
    }

    public static boolean isInOwnZone(ServerPlayer player) {
        TeamContext team = teamOf(player);
        if (team == null || player.level() != WorldManager.gameLevel) return false;
        int radius = Math.max(1, EconomyData.current().respawnZoneRadius);
        for (IRespawnPoint point : team.RespawnPoints()) {
            if (point instanceof CapturePointRespawnPoint capturePoint) {
                if (capturePoint.getObjective().GetCaptureAreaAABB().inflate(CAPTURE_AREA_MARGIN).contains(player.position())) {
                    return true;
                }
                // A respawn moved away from the area (e.g. a forward base) is a zone of its own
                if (capturePoint.getObjective().GetRespawnPosition() == null) {
                    continue;
                }
            }
            BlockPos spawn = point.getSpawnPosition();
            if (spawn != null && player.position().distanceToSqr(spawn.getCenter()) <= (double) radius * radius) {
                return true;
            }
        }
        return false;
    }
}
