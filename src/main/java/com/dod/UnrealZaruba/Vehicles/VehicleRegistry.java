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
        register(new VehicleData("pt", "vehicle.unrealzaruba.pt")
        .addItemRequirement(new ResourceLocation("unrealzaruba:skull"), 3));

        register(new VehicleData("car", "vehicle.unrealzaruba.car")
        .addItemRequirement(new ResourceLocation("unrealzaruba:skull"), 2));

        register(new VehicleData("kamikaze", "vehicle.unrealzaruba.kamikaze")
        .addItemRequirement(new ResourceLocation("unrealzaruba:skull"), 1));
    }

    public static ResourceLocation GetLocation(String vehicleName) {
        return new ResourceLocation(UnrealZaruba.MOD_ID, vehicleName);
    }
}