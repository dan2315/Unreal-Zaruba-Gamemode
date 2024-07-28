package com.dod.UnrealZaruba.TeamLogic;

import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import net.minecraft.core.BlockPos;

public class TeamEntry {
    public BlockPos blockPos;
    public BlockVolume blockVolume;

    public TeamEntry(BlockPos blockPos, BlockVolume blockVolume) {
        this.blockPos = blockPos;
        this.blockVolume = blockVolume;
    }
}
