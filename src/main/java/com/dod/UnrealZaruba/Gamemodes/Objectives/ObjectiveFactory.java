package com.dod.UnrealZaruba.Gamemodes.Objectives;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.BlockPos;

/**
 * Factory class for creating objectives from JSON based on their type.
 * This factory is used during JSON deserialization to create appropriate objective instances.
 */
public class ObjectiveFactory implements JsonDeserializer<GameObjective> {
    
    private static final Map<String, Function<JsonObject, GameObjective>> OBJECTIVE_CREATORS = new HashMap<>();
    
    static {
        // Register known objective types here
        registerObjectiveType("destructible", json -> {
            BlockVolume volume = deserializeBlockVolume(json.getAsJsonObject("volume"));
            String name = json.get("name").getAsString();
            float requiredDegreeOfDestruction = json.has("requiredDegreeOfDestruction") ? 
                json.get("requiredDegreeOfDestruction").getAsFloat() : 0.1f;
                
            DestructibleObjective objective = new DestructibleObjective(volume, name);
            if (json.has("requiredDegreeOfDestruction")) {
                objective.requiredDegreeOfDestruction = requiredDegreeOfDestruction;
            }
            return objective;
        });
        
        // Add more objective types as needed
    }
    
    /**
     * Registers a new objective type with a creator function that can instantiate it from JSON
     * 
     * @param type The type identifier string
     * @param creator Function that creates an objective from a JsonObject
     */
    public static void registerObjectiveType(String type, Function<JsonObject, GameObjective> creator) {
        OBJECTIVE_CREATORS.put(type, creator);
    }
    
    @Override
    public GameObjective deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        
        if (!jsonObject.has("type")) {
            throw new JsonParseException("Missing 'type' field in objective JSON");
        }
        
        String type = jsonObject.get("type").getAsString();
        
        Function<JsonObject, GameObjective> creator = OBJECTIVE_CREATORS.get(type);
        if (creator == null) {
            UnrealZaruba.LOGGER.error("Unknown objective type: " + type);
            throw new JsonParseException("Unknown objective type: " + type);
        }
        
        try {
            return creator.apply(jsonObject);
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("Error creating objective of type " + type, e);
            throw new JsonParseException("Error creating objective of type " + type + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to deserialize a BlockVolume from JSON
     */
    private static BlockVolume deserializeBlockVolume(JsonObject volumeJson) {
        // Extract position data from the JSON
        int x1 = volumeJson.get("x1").getAsInt();
        int y1 = volumeJson.get("y1").getAsInt();
        int z1 = volumeJson.get("z1").getAsInt();
        int x2 = volumeJson.get("x2").getAsInt();
        int y2 = volumeJson.get("y2").getAsInt();
        int z2 = volumeJson.get("z2").getAsInt();
        
        // Create BlockPos objects for the corners
        BlockPos pos1 = new BlockPos(x1, y1, z1);
        BlockPos pos2 = new BlockPos(x2, y2, z2);
        
        // Create BlockVolume using the observed constructor
        return new BlockVolume(pos1, pos2, false);
    }
} 