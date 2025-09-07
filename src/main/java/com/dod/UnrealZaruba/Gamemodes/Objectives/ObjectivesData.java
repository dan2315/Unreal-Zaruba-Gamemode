package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.AbstractGamemodeData;
import com.dod.UnrealZaruba.UnrealZaruba;

/**
 * Handler for game objectives data, managing the saving and loading of data.
 * Supports multiple types of objectives through a type field.
 */
public class ObjectivesData extends AbstractGamemodeData<ObjectivesData.ObjectivesPayload> {
    private static final String DATA_NAME = "objectives";

    /**
     * Creates a new ObjectivesData for a specific gamemode
     * 
     * @param gamemodeClass The gamemode class
     */
    public ObjectivesData(Class<? extends BaseGamemode> gamemodeClass) {
        super(ObjectivesPayload.class, gamemodeClass, DATA_NAME, new ObjectivesPayload());
    }
    
    @Override
    public Class<ObjectivesPayload> getDataClass() {
        return ObjectivesPayload.class;
    }
    
    /**
     * Save the current objectives data.
     */
    @Override
    public void saveData() throws IOException {
        super.saveData();
        UnrealZaruba.LOGGER.info("[ObjectivesData] Saved objectives data");
    }
    
    /**
     * Get the current objectives
     * 
     * @return The objectives array
     */
    public GameObjective[] getObjectives() {
        return data.getObjectivesArray();
    }
    
    /**
     * Set the objectives
     * 
     * @param objectives The new objectives
     */
    public void setObjectives(GameObjective[] objectives) {
        data.setObjectives(objectives);
        try {
            saveData();
        } catch (Exception e) {
            // Already logged in AbstractGamemodeData
        }
    }
    
    /**
     * Add an objective
     * 
     * @param objective The objective to add
     */
    public void addObjective(GameObjective objective) {
        data.addObjective(objective);
        try {
            saveData();
        } catch (Exception e) {
            // Already logged in AbstractGamemodeData
        }
    }
    
    /**
     * Remove an objective
     * 
     * @param index The index of the objective to remove
     * @return True if removed, false if index out of bounds
     */
    public boolean removeObjective(int index) {
        boolean result = data.removeObjective(index);
        if (result) {
            try {
                saveData();
            } catch (Exception e) {
                // Already logged in AbstractGamemodeData
            }
        }
        return result;
    }
    
    /**
     * Data payload class for objectives.
     * Contains all defined objectives for a gamemode.
     */
    public static class ObjectivesPayload {
        // Array of game objectives
        private GameObjective[] objectives = new GameObjective[0];
        
        /**
         * Default constructor for serialization
         */
        public ObjectivesPayload() {
            // Empty constructor for GSON
        }
        
        /**
         * Get the objectives array
         * 
         * @return Array of objectives
         */
        public GameObjective[] getObjectivesArray() {
            return objectives;
        }
        
        /**
         * Set the objectives array
         * 
         * @param objectives New objectives array
         */
        public void setObjectives(GameObjective[] objectives) {
            this.objectives = objectives != null ? objectives : new GameObjective[0];
        }
        
        /**
         * Add an objective to the array
         * 
         * @param objective The objective to add
         */
        public void addObjective(GameObjective objective) {
            if (objective == null) {
                return;
            }
            
            GameObjective[] newObjectives = new GameObjective[objectives.length + 1];
            System.arraycopy(objectives, 0, newObjectives, 0, objectives.length);
            newObjectives[objectives.length] = objective;
            objectives = newObjectives;
        }
        
        /**
         * Remove an objective at the specified index
         * 
         * @param index The index to remove
         * @return True if successful, false if index out of bounds
         */
        public boolean removeObjective(int index) {
            if (index < 0 || index >= objectives.length) {
                return false;
            }
            
            GameObjective[] newObjectives = new GameObjective[objectives.length - 1];
            System.arraycopy(objectives, 0, newObjectives, 0, index);
            System.arraycopy(objectives, index + 1, newObjectives, index, objectives.length - index - 1);
            objectives = newObjectives;
            return true;
        }
        
        /**
         * Get objective at specified index
         * 
         * @param index The index
         * @return The objective, or null if index out of bounds
         */
        public GameObjective getObjective(int index) {
            if (index < 0 || index >= objectives.length) {
                return null;
            }
            return objectives[index];
        }
        
        /**
         * Get the number of objectives
         * 
         * @return The objective count
         */
        public int getObjectiveCount() {
            return objectives.length;
        }
        
        /**
         * For serialization
         */
        public GameObjective[] getObjectives() {
            return objectives;
        }
        
        /**
         * For deserialization
         */
        public void setObjectivesArray(GameObjective[] objectives) {
            this.objectives = objectives != null ? objectives : new GameObjective[0];
        }
    }
} 