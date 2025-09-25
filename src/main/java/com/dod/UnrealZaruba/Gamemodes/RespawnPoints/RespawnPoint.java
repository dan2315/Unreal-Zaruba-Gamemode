package com.dod.unrealzaruba.Gamemodes.RespawnPoints;

import net.minecraft.core.BlockPos;

public class RespawnPoint extends BaseRespawnPoint {
    private BlockPos spawnPosition;
    private String displayName;
    private int priority;
    
    public RespawnPoint(BlockPos spawnPosition, String displayName, int priority) {
        this.spawnPosition = spawnPosition;
        this.displayName = displayName;
        this.priority = priority;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public BlockPos getSpawnPosition() {
        return spawnPosition;
    }

    @Override
    public int getPriority() {
        return priority;
    }
    
    
}
