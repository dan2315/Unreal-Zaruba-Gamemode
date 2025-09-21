package com.dod.UnrealZaruba.Vehicles;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Events.BlockStateChangedEvent;
import com.dod.UnrealZaruba.Gamemodes.RespawnPoints.BaseRespawnPoint;

import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1;
import org.valkyrienskies.core.api.ships.ServerShip;
public class Vehicle extends BaseRespawnPoint {
    private List<ServerShip> ships;
    private String vehicleType;
    private TeamColor owner;
    private boolean isAlive;
    private Consumer<Vehicle> initialization;
    private final int hitPoints;
    private int currentHP;
    HashSet<BlockPos> blocks = new HashSet<>();


    public Vehicle(String vehicleType, TeamColor owner, List<ServerShip> ships, IShipSchematicDataV1 schematicV1, Consumer<Vehicle> initialization) {
        this.vehicleType = vehicleType;
        this.owner = owner;
        this.isAlive = true;
        this.ships = ships;
        this.initialization = initialization;

        int totalBlocks = 0;
        var blockStatePalette = schematicV1.getBlockPalette();
        for (var blockData : schematicV1.getBlockData().values()) {
            UnrealZaruba.LOGGER.warn("Iterating through chunkyBlockData: {}", blockData.toString());
            for (var chunkData : blockData.getBlocks().entrySet()) {
                UnrealZaruba.LOGGER.warn("Iterating through presumably chunks, chunk is: {}", chunkData.getKey());
                totalBlocks += chunkData.getValue().size();
                for (var blockEntry : chunkData.getValue().entrySet()) {
                    blocks.add(blockEntry.getKey());

                    int id = blockEntry.getValue().getPaletteId();
                    var blockState = blockStatePalette.fromId(id);
                }
            }
        }

        hitPoints = totalBlocks;
        UnrealZaruba.LOGGER.error("Total {} health is: {}", vehicleType, hitPoints);
        currentHP = hitPoints;
    }

    public void Initialize() {
        initialization.accept(this);
    }

    public List<ServerShip> getShips() {
        return ships;
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

    public void OnBlockStateChanged(BlockStateChangedEvent event) {
        UnrealZaruba.LOGGER.warn("OnBlock changed in vehicle");
        if (event.getLevel().equals(WorldManager.gameLevel) && blocks.contains(event.getPos()) && event.getNewState().isAir()) {
            blocks.remove(event.getPos());
            currentHP -= 1;

            UnrealZaruba.LOGGER.warn("HP left: {}", (float) currentHP/hitPoints);
            if ((float) currentHP /hitPoints < 0.8f) {
                Level level = event.getLevel();
                BlockPos pos = event.getPos();
                level.explode(
                        null,                // no entity (can also pass player/entity)
                        pos.getX() + 0.5,              // center X
                        pos.getY() + 0.5,              // center Y
                        pos.getZ() + 0.5,              // center Z
                        16.0F,                          // explosion power (TNT is 4.0F)
                        Level.ExplosionInteraction.BLOCK // destroy blocks
                );
                UnrealZaruba.vehicleManager.removeVehicle(this);
            }
        }
    }
}
