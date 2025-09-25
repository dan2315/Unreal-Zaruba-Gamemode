package com.dod.unrealzaruba.Gamemodes.RespawnPoints;

import net.minecraft.core.BlockPos;

public interface IRespawnPoint {
    String getDisplayName();
    BlockPos getSpawnPosition();
    int getPriority();
    byte getRuntimeId();
}