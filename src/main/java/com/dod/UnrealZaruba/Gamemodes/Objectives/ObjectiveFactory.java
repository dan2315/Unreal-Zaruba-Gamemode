package com.dod.unrealzaruba.Gamemodes.Objectives;

import com.dod.unrealzaruba.UnrealZaruba;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager.GSON;

/**
 * Factory class for creating objectives from JSON based on their type.
 * This factory is used during JSON deserialization to create appropriate objective instances.
 */
public class ObjectiveFactory implements JsonDeserializer<GameObjective> {
    
    private static final Map<String, Function<JsonObject, GameObjective>> OBJECTIVE_CREATORS = new HashMap<>();
    
    static {
        registerObjectiveType("destructible", json -> {
            var objective = GSON.fromJson(json, DestructibleObjective.class);
            objective.InitializeAfterSerialization();
            return objective;
        });
        registerObjectiveType("capturepoint", json -> {
            var objective = GSON.fromJson(json, CapturePointObjective.class);
            objective.InitializeAfterSerialization();
            return objective;
        });
    }

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
}