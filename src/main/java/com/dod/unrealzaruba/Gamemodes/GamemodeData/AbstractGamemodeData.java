package com.dod.unrealzaruba.Gamemodes.GamemodeData;

import java.io.IOException;

import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.UnrealZaruba;

/**
 * Abstract base implementation of GamemodeData.
 * Provides common functionality for gamemode data classes.
 * 
 * @param <T> The type of data being managed
 */
public abstract class AbstractGamemodeData<T> implements GamemodeData<T> {

    private final Class<T> dataClass;
    private final Class<? extends BaseGamemode> gamemodeClass;
    private final String dataName;
    // Map settings are read from and saved to the map that was selected when they were loaded
    private final String mapKey;
    protected T data;

    public AbstractGamemodeData(Class<T> dataClass, Class<? extends BaseGamemode> gamemodeClass,
                               String dataName, T defaultData) {
        this.dataClass = dataClass;
        this.gamemodeClass = gamemodeClass;
        this.dataName = dataName;
        this.data = defaultData;
        var map = perMap() ? com.dod.unrealzaruba.WorldManager.GameMaps.selected() : null;
        this.mapKey = map == null ? null : map.key();

        GamemodeDataManager.registerHandler(gamemodeClass, this);
    }

    /** Whether this is a setting of the map (in the map's data folder) rather than of the mode. */
    protected boolean perMap() {
        return false;
    }

    private java.nio.file.Path dataDir() {
        var map = mapKey == null ? null : com.dod.unrealzaruba.WorldManager.GameMaps.get(mapKey);
        return map != null ? map.dataDir() : java.nio.file.Path.of(GamemodeDataManager.getGamemodeDataPath(gamemodeClass));
    }
    
    @Override
    public Class<T> getDataClass() {
        return dataClass;
    }
    
    @Override
    public Class<? extends BaseGamemode> getGamemodeClass() {
        return gamemodeClass;
    }
    
    @Override
    public String getDataName() {
        return dataName;
    }
    
    @Override
    public T getData() {
        return data;
    }
    
    @Override
    public void setData(T data) {
        if (data != null) {
            this.data = data;
            saveData();
        }
    }
    
    @Override
    public T loadData() {
        T loadedData = GamemodeDataManager.loadData(dataDir(), dataName, dataClass);
        if (loadedData != null) {
            UnrealZaruba.LOGGER.warn("Loaded data: {}", loadedData);
            this.data = loadedData;
            return loadedData;
        }
        return this.data;
    }
    
    @Override
    public void saveData()  {
        try {
            GamemodeDataManager.saveData(dataDir(), dataName, data);
            UnrealZaruba.LOGGER.info("[" + getClass().getSimpleName() + "] Saved data");
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[" + getClass().getSimpleName() + "] Failed to save data after update", e);
        }
    }
} 