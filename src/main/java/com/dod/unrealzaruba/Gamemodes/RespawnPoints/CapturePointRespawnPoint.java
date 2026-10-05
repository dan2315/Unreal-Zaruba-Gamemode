package com.dod.unrealzaruba.Gamemodes.RespawnPoints;

import com.dod.unrealzaruba.Gamemodes.Objectives.CapturePointObjective;
import com.dod.unrealzaruba.WorldManager.WorldManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Respawn point of a capture point, available to whichever team owns it. Spawns at the objective's configured
 * respawnPosition, or on the floor at the center of the capture area.
 */
public class CapturePointRespawnPoint extends BaseRespawnPoint {
    private static final int PRIORITY = 5;
    private static final int FLOOR_SEARCH_BELOW = 4;

    private final CapturePointObjective objective;

    public CapturePointRespawnPoint(CapturePointObjective objective) {
        this.objective = objective;
    }

    public CapturePointObjective getObjective() {
        return objective;
    }

    @Override
    public String getDisplayName() {
        return objective.GetName();
    }

    /** The block under the player's feet, like the other respawn points (teleports stand on top of it). */
    @Override
    public BlockPos getSpawnPosition() {
        BlockPos configured = objective.GetRespawnPosition();
        if (configured != null) {
            return configured.below();
        }

        var area = objective.GetCaptureArea();
        BlockPos center = area.GetCenter();
        ServerLevel level = WorldManager.gameLevel;
        if (level == null) {
            return center;
        }
        // Highest spot in the area's center column with room to stand. Areas are often drawn just above the
        // ground, so look a few blocks under them too
        for (int y = area.getMaxPos().getY(); y >= area.getMinPos().getY() - FLOOR_SEARCH_BELOW; y--) {
            BlockPos floor = new BlockPos(center.getX(), y, center.getZ());
            if (!level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
                    && level.getBlockState(floor.above()).getCollisionShape(level, floor.above()).isEmpty()
                    && level.getBlockState(floor.above(2)).getCollisionShape(level, floor.above(2)).isEmpty()) {
                return floor;
            }
        }
        return center;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
