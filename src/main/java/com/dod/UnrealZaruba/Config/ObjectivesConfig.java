package com.dod.UnrealZaruba.Config;

import java.io.File;
import java.io.IOException;

import com.dod.UnrealZaruba.Gamemodes.Objectives.GameObjective;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;

public class ObjectivesConfig extends AbstractConfig<GameObjective[]> {
    
    private static final String CONFIG_PATH = "unrealzaruba" + File.separator + "objectives.json";
    private static ObjectivesConfig instance;
    
    public static ObjectivesConfig getInstance() {
        if (instance == null) {
            instance = new ObjectivesConfig();
        }
        return instance;
    }
    
    private ObjectivesConfig() {
        // Register this config with the manager
        ConfigManager.registerConfig(GameObjective[].class, this);
    }
    
    @Override
    protected String getFilePath() {
        return CONFIG_PATH;
    }
    
    @Override
    protected Class<GameObjective[]> getConfigClass() {
        return GameObjective[].class;
    }
    
    @Override
    protected GameObjective[] getDefaultConfig() {
        return new GameObjective[0]; // Empty array as default
    }
    
    public void saveObjectives(GameObjective[] objectives) {
        try {
            save(objectives);
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Saved objectives configuration");
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Failed to save objectives configuration");
            e.printStackTrace();
        }
    }
    
    public GameObjective[] loadObjectives() {
        GameObjective[] objectives = load();
        if (objectives == null) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] No objectives found, using empty array");
            return new GameObjective[0];
        }
        return objectives;
    }
} 