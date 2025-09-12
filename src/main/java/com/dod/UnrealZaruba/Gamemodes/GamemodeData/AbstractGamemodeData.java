package com.dod.UnrealZaruba.Gamemodes.GamemodeData;

import java.io.IOException;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.UnrealZaruba;

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
    protected T data;

    public AbstractGamemodeData(Class<T> dataClass, Class<? extends BaseGamemode> gamemodeClass, 
                               String dataName, T defaultData) {
        this.dataClass = dataClass;
        this.gamemodeClass = gamemodeClass;
        this.dataName = dataName;
        this.data = defaultData;
        
        GamemodeDataManager.registerHandler(gamemodeClass, this);
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
        T loadedData = GamemodeDataManager.loadData(gamemodeClass, dataName, dataClass);
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
            GamemodeDataManager.saveData(gamemodeClass, dataName, data);
            UnrealZaruba.LOGGER.info("[" + getClass().getSimpleName() + "] Saved data");
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[" + getClass().getSimpleName() + "] Failed to save data after update", e);
        }
    }
} 