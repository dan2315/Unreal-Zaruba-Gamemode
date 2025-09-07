package com.dod.UnrealZaruba.TeamLogic;

import net.minecraft.core.BlockPos;

/**
 * Team data entry class.
 * Contains the spawn position for a team.
 */
public class TeamDataEntry {
    private BlockPos blockPos;

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
     */
    public TeamDataEntry(BlockPos blockPos) {
        this.blockPos = blockPos;
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
}
