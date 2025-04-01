package com.dod.UnrealZaruba.TeamLogic;

import java.util.List;

import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import net.minecraft.core.BlockPos;

/**
 * Team data entry class.
 * Contains the spawn position and barrier volumes for a team.
 */
public class TeamDataEntry {
    private BlockPos blockPos;
    private List<BlockVolume> barrierVolumes;

    /**
     * Default constructor for JSON deserialization
     */
    public TeamDataEntry() {
        // Default constructor for JSON deserialization
    }

    /**
     * Creates a new team data entry with the specified spawn position and barrier volumes
     *
     * @param blockPos The spawn position
     * @param barrierVolumes The barrier volumes
     */
    public TeamDataEntry(BlockPos blockPos, List<BlockVolume> barrierVolumes) {
        this.blockPos = blockPos;
        this.barrierVolumes = barrierVolumes;
    }
    
    /**
     * Gets the spawn position
     * 
     * @return The spawn position
     */
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    /**
     * Sets the spawn position
     * 
     * @param blockPos The spawn position to set
     */
    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }
    
    /**
     * Gets the barrier volumes
     * 
     * @return The barrier volumes
     */
    public List<BlockVolume> getBarrierVolumes() {
        return barrierVolumes;
    }
    
    /**
     * Sets the barrier volumes
     * 
     * @param barrierVolumes The barrier volumes to set
     */
    public void setBarrierVolumes(List<BlockVolume> barrierVolumes) {
        this.barrierVolumes = barrierVolumes;
    }
}
