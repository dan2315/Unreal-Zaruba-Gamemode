package com.dod.UnrealZaruba.Config;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;

/**
 * Abstract base class for configuration handling.
 * Provides common functionality for saving, loading, and creating default configurations.
 * 
 * @param <T> The type of the configuration object
 */
public abstract class AbstractConfig<T> {
    
    /**
     * Gets a specific configuration by its data class
     * 
     * @param <C> The type of configuration data
     * @param configClass The class of the configuration data
     * @return The configuration instance
     */
    public static <C> AbstractConfig<C> get(Class<C> configClass) {
        return ConfigManager.get(configClass);
    }
    
    /**
     * Gets the file path for this configuration
     * 
     * @return The file path as a String
     */
    protected abstract String getFilePath();
    
    /**
     * Gets the class of the configuration object
     * 
     * @return The class of the configuration object
     */
    protected abstract Class<T> getConfigClass();
    
    /**
     * Creates a default configuration object
     * 
     * @return A default configuration object
     */
    protected abstract T getDefaultConfig();
    
    /**
     * Saves the configuration to file
     * 
     * @param config The configuration object to save
     * @throws IOException If an I/O error occurs
     */
    public void save(T config) throws IOException {
        createDirectoryIfNotExist(getFilePath());
        try (FileWriter writer = new FileWriter(getFilePath())) {
            ConfigManager.getGson().toJson(config, writer);
        }
    }
    
    /**
     * Loads the configuration from file
     * 
     * @return The loaded configuration object, or null if an error occurs
     */
    public T load() {
        try (FileReader reader = new FileReader(getFilePath())) {
            T config = ConfigManager.getGson().fromJson(reader, getConfigClass());
            if (config != null) {
                UnrealZaruba.LOGGER.info("[UnrealZaruba] Loaded config: " + getFilePath());
                return config;
            }
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Config file not found: " + getFilePath());
        }
        return null;
    }
    
    /**
     * Creates a default configuration file if it doesn't exist
     * 
     * @return The default or loaded configuration
     */
    public T createDefaultIfNotExist() {
        try {
            File file = new File(getFilePath());
            if (!file.exists()) {
                T defaultConfig = getDefaultConfig();
                createDirectoryIfNotExist(getFilePath());
                save(defaultConfig);
                UnrealZaruba.LOGGER.info("[UnrealZaruba] Created default config: " + getFilePath());
                return defaultConfig;
            } else {
                return load();
            }
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Failed to create default config: " + getFilePath());
            e.printStackTrace();
            return getDefaultConfig();
        }
    }
    
    /**
     * Creates the directory structure if it doesn't exist
     * 
     * @param filePath The file path to create directories for
     */
    private void createDirectoryIfNotExist(String filePath) {
        File file = new File(filePath);
        File directory = file.getParentFile();

        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }
    }
}