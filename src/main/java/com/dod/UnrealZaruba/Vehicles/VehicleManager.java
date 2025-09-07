package com.dod.UnrealZaruba.Vehicles;

import java.util.ArrayList;
import java.util.List;

public class VehicleManager {
    private final List<Vehicle> vehicles = new ArrayList<>();
    
    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
    }

    public Vehicle getVehicle(int index) {
        return vehicles.get(index);
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void removeVehicle(Vehicle vehicle) {
        vehicles.remove(vehicle);
    }

    public void clearVehicles() {
        vehicles.clear();
    }
    
}
