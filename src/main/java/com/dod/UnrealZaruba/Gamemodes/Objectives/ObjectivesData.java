package com.dod.UnrealZaruba.Gamemodes.Objectives;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.AbstractGamemodeData;
import com.dod.UnrealZaruba.UnrealZaruba;

public class ObjectivesData extends AbstractGamemodeData<ObjectivesData.ObjectivesPayload> {
    private static final String DATA_NAME = "objectives";

    public ObjectivesData(Class<? extends BaseGamemode> gamemodeClass) {
        super(ObjectivesPayload.class, gamemodeClass, DATA_NAME, new ObjectivesPayload());
    }
    
    @Override
    public Class<ObjectivesPayload> getDataClass() {
        return ObjectivesPayload.class;
    }

    @Override
    public void saveData() {
        super.saveData();
        UnrealZaruba.LOGGER.info("[ObjectivesData] Saved objectives data");
    }
    

    public GameObjective[] getObjectives() {
        return data.getObjectivesArray();
    }

    public void setObjectives(GameObjective[] objectives) {
        data.setObjectives(objectives);
        try {
            saveData();
        } catch (Exception e) {
            // Already logged in AbstractGamemodeData
        }
    }

    public void addObjective(GameObjective objective) {
        data.addObjective(objective);
        try {
            saveData();
        } catch (Exception e) {
            // Already logged in AbstractGamemodeData
        }
    }

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