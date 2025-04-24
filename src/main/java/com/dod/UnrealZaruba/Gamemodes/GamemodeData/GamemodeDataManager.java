package com.dod.UnrealZaruba.Gamemodes.GamemodeData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.Objectives.GameObjective;
import com.dod.UnrealZaruba.Gamemodes.Objectives.ObjectiveFactory;
import com.dod.UnrealZaruba.TeamLogic.TeamData;
import com.dod.UnrealZaruba.TeamLogic.TeamDataHandler;
import com.dod.UnrealZaruba.Gamemodes.Objectives.ObjectivesData;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnDataHandler;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Manages gamemode-specific data storage and retrieval.
 * Organizes data in folders by gamemode name.
 */
public class GamemodeDataManager {
    private static final String BASE_PATH = "unrealzaruba" + File.separator + "gamemodedata";
    private static final Map<Class<?>, Object> dataHandlers = new HashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(GameObjective.class, new ObjectiveFactory())
            .setPrettyPrinting()
            .create();

    static {
        registerDataHandler(TeamData.class, new TeamDataHandler(BaseGamemode.class));
        registerDataHandler(VehicleSpawnData.class, new VehicleSpawnDataHandler(BaseGamemode.class));
        registerDataHandler(GameObjective[].class, new ObjectivesData(BaseGamemode.class));
    }
    
    public static <T> void registerDataHandler(Class<T> dataClass, Object handler) {
        dataHandlers.put(dataClass, handler);
    }
    
    @SuppressWarnings("unchecked")
    public static <T, H> H getDataHandler(Class<T> dataClass, Class<H> handlerClass) {
        Object handler = dataHandlers.get(dataClass);
        if (handler != null && handlerClass.isInstance(handler)) {
            return (H) handler;
        }
        return null;
    }
    
    public static String getGamemodeDataPath(Class<? extends BaseGamemode> gamemodeClass) {
        return BASE_PATH + File.separator + gamemodeClass.getSimpleName();
    }

    public static <T> void saveData(Class<? extends BaseGamemode> gamemodeClass, String dataName, T data) throws IOException {
        String directoryPath = getGamemodeDataPath(gamemodeClass);
        Path dirPath = Paths.get(directoryPath);
        
        // Create directories if they don't exist
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        String filePath = directoryPath + File.separator + dataName + ".json";
        try (FileWriter writer = new FileWriter(filePath)) {
            GSON.toJson(data, writer);
            UnrealZaruba.LOGGER.info("[GamemodeDataManager] Saved data: " + filePath);
        }
    }

    public static <T> T loadData(Class<? extends BaseGamemode> gamemodeClass, String dataName, Class<T> dataClass) {
        String filePath = getGamemodeDataPath(gamemodeClass) + File.separator + dataName + ".json";
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