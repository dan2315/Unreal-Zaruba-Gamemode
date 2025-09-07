package com.dod.UnrealZaruba.ModBlocks.Tent;

import com.dod.UnrealZaruba.Gamemodes.RespawnPoints.IRespawnPoint;

import net.minecraft.core.BlockPos;

public class Tent implements IRespawnPoint {
    public BlockPos spawn_point;

    public Tent(BlockPos spawn_point) {
        this.spawn_point = spawn_point;
    }

    @Override
    public String getDisplayName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDisplayName'");
    }

    @Override
    public BlockPos getSpawnPosition() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSpawnPosition'");
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPriority'");
    }
}
