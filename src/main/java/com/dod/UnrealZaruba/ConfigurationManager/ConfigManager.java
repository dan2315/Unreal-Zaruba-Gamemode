package com.dod.UnrealZaruba.ConfigurationManager;

import java.util.HashMap;
import java.util.Map;

import com.dod.UnrealZaruba.Config.AbstractConfig;
import com.dod.UnrealZaruba.Config.DestructibleObjectiveDeserializer;
import com.dod.UnrealZaruba.Config.DestructibleObjectivesConfig;
import com.dod.UnrealZaruba.Config.MainConfig;
import com.dod.UnrealZaruba.Config.TeamsConfig;
import com.dod.UnrealZaruba.Config.MainConfig.MainConfigData;
import com.dod.UnrealZaruba.Config.MainConfig.Mode;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjective;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigManager {
    
    private static final Map<Class<?>, AbstractConfig<?>> configInstances = new HashMap<>();
    
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(DestructibleObjective.class, new DestructibleObjectiveDeserializer())
            .setPrettyPrinting()
            .create();
    
    public static Gson getGson() {
        return gson;
    }
    
    public static <T> void registerConfig(Class<T> configClass, AbstractConfig<T> config) {
        configInstances.put(configClass, config);
        UnrealZaruba.LOGGER.debug("[UnrealZaruba] Registered config: " + configClass.getSimpleName());
    }
    
    @SuppressWarnings("unchecked")
    public static <T> AbstractConfig<T> get(Class<T> configClass) {
        return (AbstractConfig<T>) configInstances.get(configClass);
    }

    public static void init() {
        // Initialize all configs
        MainConfig.getInstance();
        DestructibleObjectivesConfig.getInstance();
        TeamsConfig.getInstance();
        
        // Create default configurations if they don't exist
        getMainConfig().createDefaultIfNotExist();
        
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Configuration system initialized");
    }

    public static MainConfig getMainConfig() {
        return MainConfig.getInstance();
    }

    public static DestructibleObjectivesConfig getObjectivesConfig() {
        return DestructibleObjectivesConfig.getInstance();
    }

    public static TeamsConfig getTeamsConfig() {
        return TeamsConfig.getInstance();
    }

    public static boolean isDevMode() {
        return getMainConfig().getMode() == Mode.DEV;
    }

    public static void setDevMode() {
        getMainConfig().setMode(Mode.DEV);
    }

    public static void setGameMode() {
        getMainConfig().setMode(Mode.GAME);
    }

    public static void saveAllConfigs() {
        try {
            // Save main config
            MainConfig.MainConfigData mainConfigData = getMainConfig().loadMainConfig();
            getMainConfig().saveMainConfig(mainConfigData);
            
            // Save objectives and teams if available using their handlers
            getObjectivesConfig().loadObjectives();
            getTeamsConfig().loadTeamData();
            
            UnrealZaruba.LOGGER.info("[UnrealZaruba] All configurations saved successfully");
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Failed to save all configurations", e);
        }
    }
} 