package com.dod.UnrealZaruba.Vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dod.UnrealZaruba.UnrealZaruba;

import net.minecraft.resources.ResourceLocation;

public class VehicleRegistry {
    private static final Map<String, VehicleData> VEHICLES = new HashMap<>();

    public static void register(VehicleData vehicleData) {
        VEHICLES.put(vehicleData.getKey().toString(), vehicleData);
    }

    public static VehicleData getVehicle(String vehicleName) {
        return VEHICLES.get(vehicleName);
    }

    public static List<String> getVehicleKeys() {
        return new ArrayList<>(VEHICLES.keySet());
    }

    public static void init() {
        register(new VehicleData("test", "Vehicle 1")
        .addItemRequirement(new ResourceLocation("minecraft:gold_ingot"), 8));

        register(new VehicleData("test1", "Tarahtelka")
        .addItemRequirement(new ResourceLocation("minecraft:iron_ingot"), 16)
        .addItemRequirement(new ResourceLocation("minecraft:oak_planks"), 16));

        register(new VehicleData("test2", "Lobster")
        .addItemRequirement(new ResourceLocation("minecraft:diamond"), 8)
        .addItemRequirement(new ResourceLocation("minecraft:netherite_ingot"), 1));
    }

    public static ResourceLocation GetLocation(String vehicleName) {
        return new ResourceLocation(UnrealZaruba.MOD_ID, "schematics/" + vehicleName);
    }
}