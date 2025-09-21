package com.dod.UnrealZaruba.ModBlocks.Tent;

import com.dod.UnrealZaruba.Gamemodes.RespawnPoints.BaseRespawnPoint;
import com.dod.UnrealZaruba.Gamemodes.RespawnPoints.IRespawnPoint;

import net.minecraft.core.BlockPos;

public class Tent extends BaseRespawnPoint {
    public BlockPos spawn_point;

    public Tent(BlockPos spawn_point) {
        this.spawn_point = spawn_point;
    }

    @Override
    public String getDisplayName() {
        return  "Палатка";
    }

    @Override
    public BlockPos getSpawnPosition() {
        return spawn_point;
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
