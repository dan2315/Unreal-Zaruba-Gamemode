package com.dod.UnrealZaruba.Config;

import java.io.File;
import java.io.IOException;

import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjective;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;

public class DestructibleObjectivesConfig extends AbstractConfig<DestructibleObjective[]> {
    
    private static final String CONFIG_PATH = "unrealzaruba" + File.separator + "destructibleObjectives.json";
    private static DestructibleObjectivesConfig instance;
    
    public static DestructibleObjectivesConfig getInstance() {
        if (instance == null) {
            instance = new DestructibleObjectivesConfig();
        }
        return instance;
    }
    
    private DestructibleObjectivesConfig() {
        // Register this config with the manager
        ConfigManager.registerConfig(DestructibleObjective[].class, this);
    }
    
    @Override
    protected String getFilePath() {
        return CONFIG_PATH;
    }
    
    @Override
    protected Class<DestructibleObjective[]> getConfigClass() {
        return DestructibleObjective[].class;
    }
    
    @Override
    protected DestructibleObjective[] getDefaultConfig() {
        return new DestructibleObjective[0]; // Empty array as default
    }
    
    public void saveObjectives(DestructibleObjective[] objectives) {
        try {
            save(objectives);
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Saved destructible objectives configuration");
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Failed to save destructible objectives configuration");
            e.printStackTrace();
        }
    }
    
    public DestructibleObjective[] loadObjectives() {
        DestructibleObjective[] objectives = load();
        if (objectives == null) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] No destructible objectives found, using empty array");
            return new DestructibleObjective[0];
        }
        return objectives;
    }
} 