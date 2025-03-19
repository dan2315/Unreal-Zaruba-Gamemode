package com.dod.UnrealZaruba.TeamLogic;

import java.util.List;

import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import net.minecraft.core.BlockPos;

/**
 * Team data entry point.
 */
public class TeamDataEntry {
    public BlockPos blockPos;
    public List<BlockVolume> barrierVolumes;

    /**
     * Instantiates a new Team data entry.
     *
     * @param blockPos      the block pos
     * @param barrierVolume the barrier volume
     */
    public TeamDataEntry(BlockPos blockPos, List<BlockVolume> barrierVolume) {
        this.blockPos = blockPos;
       barrierVolumes = barrierVolume;
    }
}
