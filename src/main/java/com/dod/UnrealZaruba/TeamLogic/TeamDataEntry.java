package com.dod.UnrealZaruba.TeamLogic;

import java.util.List;

import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import net.minecraft.core.BlockPos;

public class TeamDataEntry {
    public BlockPos blockPos;
    public List<BlockVolume> barrierVolumes;

    public TeamDataEntry(BlockPos blockPos, List<BlockVolume> barrierVolume) {
        this.blockPos = blockPos;
       barrierVolumes = barrierVolume;
    }
}
