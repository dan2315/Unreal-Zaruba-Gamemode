package com.dod.UnrealZaruba.Vehicles;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.RespawnPoints.IRespawnPoint;

import java.util.List;

import net.minecraft.core.BlockPos;
import org.valkyrienskies.core.api.ships.ServerShip;
public class Vehicle implements IRespawnPoint {
    private List<ServerShip> ships;
    private String vehicleType;
    private TeamColor owner;
    private boolean isAlive = false;
    private Runnable initialization;


    public Vehicle(String vehicleType, TeamColor owner, List<ServerShip> ships, Runnable initialization) {
        this.vehicleType = vehicleType;
        this.owner = owner;
        this.isAlive = true;
        this.ships = ships;
        this.initialization = initialization;
    }

    public void Initialize() {
        initialization.run();
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }

    public TeamColor getOwner() {
        return owner;
    }

    public void setOwner(TeamColor owner) {
        this.owner = owner;
    }

    @Override
    public String getDisplayName() {
        return "Vehicle";
    }

    @Override
    public BlockPos getSpawnPosition() {
        return new BlockPos(0, 0, 0);
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
