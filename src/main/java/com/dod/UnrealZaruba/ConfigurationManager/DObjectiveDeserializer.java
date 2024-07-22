package com.dod.UnrealZaruba.ConfigurationManager;

import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjective;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

public class DObjectiveDeserializer implements JsonDeserializer<DestructibleObjective> {
    @Override
    public DestructibleObjective deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        JsonObject jsonObject = json.getAsJsonObject();

        BlockVolume volume = context.deserialize(jsonObject.get("volume"), BlockVolume.class);
        String name = jsonObject.get("name").getAsString();

        return new DestructibleObjective(volume, name);
    }
}
