package com.dod.UnrealZaruba.ConfigurationManager;

import java.util.HashMap;
import java.util.Map;

import com.dod.UnrealZaruba.Config.AbstractConfig;
import com.dod.UnrealZaruba.Config.MainConfig;
import com.dod.UnrealZaruba.Config.MainConfig.MainConfigData;
import com.dod.UnrealZaruba.Config.MainConfig.Mode;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjective;
import com.dod.UnrealZaruba.Gamemodes.Objectives.GameObjective;
import com.dod.UnrealZaruba.Gamemodes.Objectives.ObjectiveFactory;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigManager {
    
    private static final Map<Class<?>, AbstractConfig<?>> configInstances = new HashMap<>();
    
    private static final Gson gson = new GsonBuilder()
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
        
        // Create default configurations if they don't exist
        getMainConfig().createDefaultIfNotExist();
        
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Configuration system initialized");
    }

    public static MainConfig getMainConfig() {
        return MainConfig.getInstance();
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
            
            UnrealZaruba.LOGGER.info("[UnrealZaruba] All configurations saved successfully");
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Failed to save all configurations", e);
        }
    }
} 