package com.dod.UnrealZaruba.ModBlocks.VehicleSpawn;

import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.VsIntegration.ShipCreator;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;

/**
 * Block entity for the VehicleSpawnBlock that stores vehicle type and other data.
 */
public class VehicleSpawnBlockEntity extends BlockEntity {
    private String vehicleType = "default"; // Type of vehicle to spawn
    private TeamColor teamColor = TeamColor.RED; // Team color for the vehicle
    
    public VehicleSpawnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.VEHICLE_SPAWN_BLOCK_ENTITY.get(), pos, state);
    }
    
    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
        setChanged();
    }
    
    public String getVehicleType() {
        return vehicleType;
    }
    
    public void setTeamColor(TeamColor teamColor) {
        this.teamColor = teamColor;
        setChanged();
        // When team color changes, update the registration
        if (level != null && !level.isClientSide) {
            registerWithDataHandler();
        }
    }
    
    public TeamColor getTeamColor() {
        return teamColor;
    }
    
    public void onPlaced() {
        if (level != null && !level.isClientSide) {
            registerWithDataHandler();
        }
    }
    
    public void onRemoved() {
        if (level != null) {
            VehicleSpawnDataHandler handler = getDataHandler();
            if (handler != null) {
                handler.unregisterBlock(worldPosition, level.dimension());
            }
        }
    }
    
    private void registerWithDataHandler() {
        if (level != null && !level.isClientSide) {
            VehicleSpawnDataHandler handler = getDataHandler();
            if (handler != null) {
                handler.registerBlock(worldPosition, level.dimension(), vehicleType, teamColor);
            }
        }
    }

    private VehicleSpawnDataHandler getDataHandler() {
        return GamemodeDataManager.getDataHandler(VehicleSpawnData.class, VehicleSpawnDataHandler.class);
    }

    public void spawnVehicle() {
        if (level != null && !level.isClientSide) {
            UnrealZaruba.LOGGER.info("[VehicleSpawnBlockEntity] Spawning vehicle of type: " + vehicleType + 
                                    " for team: " + teamColor + " at " + worldPosition);
            
            if (level instanceof ServerLevel serverLevel) {
                boolean success = ShipCreator.CreateShipFromTemplate(worldPosition, new ResourceLocation(vehicleType), serverLevel);
                
                if (!success) {
                    UnrealZaruba.LOGGER.error("[VehicleSpawnBlockEntity] Failed to spawn vehicle of type: " + vehicleType);
                }
            } else {
                UnrealZaruba.LOGGER.error("[VehicleSpawnBlockEntity] Failed to spawn vehicle, level is not a ServerLevel");
            }
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("VehicleType", vehicleType);
        tag.putString("TeamColor", teamColor.name());
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("VehicleType")) {
            vehicleType = tag.getString("VehicleType");
        }
        if (tag.contains("TeamColor")) {
            try {
                teamColor = TeamColor.valueOf(tag.getString("TeamColor"));
            } catch (IllegalArgumentException e) {
                teamColor = TeamColor.RED; // Default to RED if invalid
                UnrealZaruba.LOGGER.warn("[VehicleSpawnBlockEntity] Invalid team color in NBT data: " + tag.getString("TeamColor"));
            }
        }
    }
} 