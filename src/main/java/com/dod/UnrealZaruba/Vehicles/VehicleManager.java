package com.dod.UnrealZaruba.Vehicles;

import com.dod.UnrealZaruba.Events.BlockStateChangedEvent;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

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
        var shipsToDelete = vehicle.getShips();
        for (var ship : shipsToDelete) {
            VSGameUtilsKt.getShipObjectWorld(WorldManager.server).deleteShip(ship);
        }
        vehicles.remove(vehicle);
    }

    public void clearVehicles() {
        vehicles.clear();
    }

    public void TickVehicles() {

    }

    public void OnBlockStateChanged(BlockStateChangedEvent event) {
        for (Vehicle vehicle : vehicles) {
            vehicle.OnBlockStateChanged(event);
        }
    }
}
