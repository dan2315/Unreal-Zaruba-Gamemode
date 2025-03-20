package com.dod.UnrealZaruba.Gamemodes.Objectives;

import net.minecraft.server.level.ServerPlayer;

/**
 * Interface for displaying progress of game objectives to players.
 * Implementations can include boss bars, scoreboard displays, etc.
 */
public interface IProgressDisplay {
    /**
     * Updates the displayed progress value
     * 
     * @param progress Progress value between 0.0 and 1.0
     */
    void updateProgress(float progress);
    
    /**
     * Updates visibility of the progress display for a specific player
     * 
     * @param player The player to update visibility for
     */
    void updatePlayerVisibility(ServerPlayer player);
    
    /**
     * Sets the activation distance for this progress display
     * 
     * @param distance The squared distance at which the display becomes visible
     */
    void setActivationDistance(float distance);
    
    /**
     * Clears all player associations and resets the display
     */
    void clear();
} 