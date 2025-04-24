package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.io.IOException;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.UnrealZaruba;

/**
 * Handler for game objectives data, managing the saving and loading of data.
 * Supports multiple types of objectives through a type field.
 */
public class ObjectivesData {
    private static final String DATA_NAME = "objectives";
    private GameObjective[] objectives;
    private final Class<? extends BaseGamemode> gamemodeClass;
    
    public ObjectivesData(Class<? extends BaseGamemode> gamemodeClass) {
        this.gamemodeClass = gamemodeClass;
        this.objectives = new GameObjective[0];
    }
    
    /**
     * Save the current objectives data.
     */
    public void saveData() {
        try {
            GamemodeDataManager.saveData(gamemodeClass, DATA_NAME, objectives);
            UnrealZaruba.LOGGER.info("[ObjectivesData] Saved objectives data");
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[ObjectivesData] Failed to save objectives data", e);
        }
    }
    
    /**
     * Load objectives data.
     * 
     * @return The loaded objectives data, or a new empty array if none exists
     */
    public GameObjective[] loadData() {
        GameObjective[] loadedData = GamemodeDataManager.loadData(gamemodeClass, DATA_NAME, GameObjective[].class);
        if (loadedData != null) {
            this.objectives = loadedData;
            return loadedData;
        } else {
            this.objectives = new GameObjective[0];
            return this.objectives;
        }
    }
    
    /**
     * Get the current objectives data.
     * 
     * @return The current objectives array
     */
    public GameObjective[] getObjectives() {
        return objectives;
    }
    
    /**
     * Update the objectives data.
     * 
     * @param objectives The new objectives array
     */
    public void setObjectives(GameObjective[] objectives) {
        this.objectives = objectives;
        saveData();
    }
} 