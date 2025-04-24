package com.dod.UnrealZaruba.TeamLogic;

import java.io.IOException;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.UnrealZaruba;

/**
 * Handler for team data, managing the saving and loading of team spawn locations.
 */
public class TeamDataHandler {
    private static final String DATA_NAME = "teamData";
    private TeamData data;
    private final Class<? extends BaseGamemode> gamemodeClass;
    
    public TeamDataHandler(Class<? extends BaseGamemode> gamemodeClass) {
        this.gamemodeClass = gamemodeClass;
        this.data = new TeamData();
    }
    
    /**
     * Save the current team data.
     */
    public void saveData() {
        try {
            GamemodeDataManager.saveData(gamemodeClass, DATA_NAME, data);
            UnrealZaruba.LOGGER.info("[TeamDataHandler] Saved team data");
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[TeamDataHandler] Failed to save team data", e);
        }
    }
    
    /**
     * Load team data.
     * 
     * @return The loaded team data, or a new empty data if none exists
     */
    public TeamData loadData() {
        TeamData loadedData = GamemodeDataManager.loadData(gamemodeClass, DATA_NAME, TeamData.class);
        if (loadedData != null) {
            this.data = loadedData;
            return loadedData;
        } else {
            this.data = new TeamData();
            return this.data;
        }
    }
    
    /**
     * Get the current team data.
     * 
     * @return The current team data
     */
    public TeamData getData() {
        return data;
    }
    
    /**
     * Update the team data.
     * 
     * @param teamData The new team data
     */
    public void setData(TeamData teamData) {
        if (teamData != null) {
            this.data = teamData;
            saveData();
        }
    }
} 