package com.dod.unrealzaruba.Gamemodes.GamemodeData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.Objectives.GameObjective;
import com.dod.unrealzaruba.Gamemodes.Objectives.ObjectiveFactory;
import com.dod.unrealzaruba.UnrealZaruba;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Manages gamemode-specific data storage and retrieval.
 * Organizes data in folders by gamemode name.
 */
public class GamemodeDataManager {
    private static final String BASE_PATH = "unrealzaruba" + File.separator + "gamemodedata";
    private static final Map<Class<? extends BaseGamemode>, Map<Class<? extends GamemodeData<?>>, GamemodeData<?>>> dataRegistry = new HashMap<>();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(GameObjective.class, new ObjectiveFactory())
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    
    public static <T extends GamemodeData<?>> void registerHandler(Class<? extends BaseGamemode> gamemodeClass, T dataHandler) {
        Map<Class<? extends GamemodeData<?>>, GamemodeData<?>> gamemodeMap = dataRegistry.computeIfAbsent(gamemodeClass, k -> new HashMap<>());
        gamemodeMap.put((Class<? extends GamemodeData<?>>) dataHandler.getClass(), dataHandler);
        dataHandler.loadData();
    }

    public static <D> D getHandler(Class<? extends BaseGamemode> gamemodeClass, Class<D> dataClass) {
        Map<Class<? extends GamemodeData<?>>, GamemodeData<?>> gamemodeMap = dataRegistry.get(gamemodeClass);
        UnrealZaruba.LOGGER.debug("[GamemodeDataManager] Getting handler for " + gamemodeClass.getSimpleName() + " and " + dataClass.getSimpleName());
        if (gamemodeMap != null) {
            GamemodeData<?> dataHandler = gamemodeMap.get(dataClass);
            UnrealZaruba.LOGGER.debug("[GamemodeDataManager] Data: " + dataHandler);
            if (dataHandler != null) {
                return (D) dataHandler;
            }
        }
        return null;
    }

    public static String getGamemodeDataPath(Class<? extends BaseGamemode> gamemodeClass) {
        return BASE_PATH + File.separator + gamemodeClass.getSimpleName();
    }

    /** Writes {@code <dataName>.json} into a mode's or a map's data folder. */
    public static <T> void saveData(Path dirPath, String dataName, T data) throws IOException {
        
        // Create directories if they don't exist
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        String filePath = dirPath.resolve(dataName + ".json").toString();
        try (FileWriter writer = new FileWriter(filePath)) {
            GSON.toJson(data, writer);
            UnrealZaruba.LOGGER.info("[GamemodeDataManager] Saved data: " + filePath);
        }
    }

    public static <T> T loadData(Path dirPath, String dataName, Class<T> dataClass) {
        String filePath = dirPath.resolve(dataName + ".json").toString();
        File file = new File(filePath);
        
        if (!file.exists()) {
            UnrealZaruba.LOGGER.info("[GamemodeDataManager] No data file found: " + filePath);
            return null;
        }
        
        try (FileReader reader = new FileReader(file)) {
            T data = GSON.fromJson(reader, dataClass);
            UnrealZaruba.LOGGER.info("[GamemodeDataManager] Loaded data: " + filePath);
            return data;
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[GamemodeDataManager] Error loading data: " + filePath, e);
            return null;
        }
    }

    public static boolean dataExists(Class<? extends BaseGamemode> gamemodeClass, String dataName) {
        String filePath = getGamemodeDataPath(gamemodeClass) + File.separator + dataName + ".json";
        return new File(filePath).exists();
    }
} 