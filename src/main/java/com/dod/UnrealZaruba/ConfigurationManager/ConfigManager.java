package com.dod.UnrealZaruba.ConfigurationManager;

import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjective;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    final static String directoryPath = "unrealzaruba";
    public final static String Objectives = directoryPath + File.pathSeparator + "destructibleObjectives.json";
    public final static String Teams = directoryPath + File.pathSeparator + "teams.json";
    public final static String Tokens = directoryPath + File.pathSeparator + "tokens.json";

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(DestructibleObjective.class, new DObjectiveDeserializer()).setPrettyPrinting()
            .create();

    public static <T> void saveConfig(String filePath, T config) throws IOException {
        CreateDirectoryIfNotExist(filePath);

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(config, writer);
        }
    }

    public static <T> T loadConfig(String filePath, Class<T> configClass) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, configClass);
        }
    }

    public static <T> void createDefaultConfig(String filePath, T defaultConfig) throws IOException {
        CreateDirectoryIfNotExist(filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            saveConfig(filePath, defaultConfig);
        }
    }

    private static void CreateDirectoryIfNotExist(String filePath) {
        File file = new File(filePath);
        File directory = file.getParentFile();

        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }
    }
}
