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
     * Save the current objectives' data.
     */
    @Override
    public void saveData() {
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

    public static class ObjectivesPayload {
        private GameObjective[] objectives = new GameObjective[0];

        public ObjectivesPayload() {
            // Empty constructor for GSON
        }
        
        public GameObjective[] getObjectivesArray() {
            return objectives;
        }
        
        public void setObjectives(GameObjective[] objectives) {
            this.objectives = objectives != null ? objectives : new GameObjective[0];
        }

        public void addObjective(GameObjective objective) {
            if (objective == null) {
                return;
            }
            
            GameObjective[] newObjectives = new GameObjective[objectives.length + 1];
            System.arraycopy(objectives, 0, newObjectives, 0, objectives.length);
            newObjectives[objectives.length] = objective;
            objectives = newObjectives;
        }

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

        public GameObjective getObjective(int index) {
            if (index < 0 || index >= objectives.length) {
                return null;
            }
            return objectives[index];
        }

        public int getObjectiveCount() {
            return objectives.length;
        }

        public GameObjective[] getObjectives() {
            return objectives;
        }

        public void setObjectivesArray(GameObjective[] objectives) {
            this.objectives = objectives != null ? objectives : new GameObjective[0];
        }
    }
} 