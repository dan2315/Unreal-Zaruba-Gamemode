package com.dod.UnrealZaruba.Gamemodes.GamemodeData;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.Objectives.ObjectivesData;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;
import com.dod.UnrealZaruba.TeamLogic.TeamData;
import com.dod.UnrealZaruba.UnrealZaruba;

/**
 * Factory for initializing gamemode data instances.
 * Creates and registers data handlers for gamemodes.
 */
public class GamemodeDataFactory {
    
    /**
     * Initialize all data handlers for a gamemode
     * 
     * @param gamemodeClass The gamemode class to initialize data handlers for
     */
    public static void initializeGamemodeData(Class<? extends BaseGamemode> gamemodeClass) {
        UnrealZaruba.LOGGER.info("Initializing gamemode data for " + gamemodeClass.getSimpleName());
        
        // Create and register data handlers
        new TeamData(gamemodeClass);
        new VehicleSpawnData(gamemodeClass);
        new ObjectivesData(gamemodeClass);
        
        // Load data for all handlers
        loadAllData(gamemodeClass);
    }
    
    /**
     * Load all data for a gamemode
     * 
     * @param gamemodeClass The gamemode class to load data for
     */
    public static void loadAllData(Class<? extends BaseGamemode> gamemodeClass) {
        // Get and load team data using its payload class
        TeamData teamData = GamemodeDataManager.getHandler(gamemodeClass, 
            TeamData.class);
        if (teamData != null) {
            teamData.loadData();
        }
        
        // Get and load vehicle spawn data
        VehicleSpawnData vehicleData = GamemodeDataManager.getHandler(gamemodeClass, 
            VehicleSpawnData.class);
        if (vehicleData != null) {
            vehicleData.loadData();
        }
        
        // Get and load objectives data
        ObjectivesData objectivesData = GamemodeDataManager.getHandler(gamemodeClass, 
            ObjectivesData.class);
        if (objectivesData != null) {
            objectivesData.loadData();
        }
    }
} 