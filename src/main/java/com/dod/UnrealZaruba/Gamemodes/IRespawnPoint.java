package com.dod.UnrealZaruba.Gamemodes;

import net.minecraft.core.BlockPos;

public interface IRespawnPoint {
    String getDisplayName();
    BlockPos getSpawnPoint();
    int getPriority();
}