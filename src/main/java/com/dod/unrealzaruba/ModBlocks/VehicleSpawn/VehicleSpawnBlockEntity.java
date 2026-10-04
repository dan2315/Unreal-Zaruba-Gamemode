package com.dod.unrealzaruba.ModBlocks.VehicleSpawn;

import net.minecraft.core.HolderLookup;
import com.dod.unrealzaruba.ModBlocks.ModBlocks;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.SableIntegration.ShipCreator;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.dod.unrealzaruba.Commands.Arguments.TeamColor;

public class VehicleSpawnBlockEntity extends BlockEntity {
    private String vehicleType = "default";
    private TeamColor teamColor = TeamColor.RED;
    
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
        if (level != null && !level.isClientSide) {
            registerWithDataHandler();
        }
    }
    
    public TeamColor getTeamColor() {
        return teamColor;
    }
    
    private void registerWithDataHandler() {
        if (level != null && !level.isClientSide) {
            VehicleSpawnData handler = getDataHandler();
            if (handler != null) {
                handler.addLocation(worldPosition, level.dimension(), vehicleType, teamColor);
            }
        }
    }

    private VehicleSpawnData getDataHandler() {
        BaseGamemode activeGamemode = GamemodeManager.instance.GetActiveGamemode();
        return GamemodeDataManager.getHandler(activeGamemode.getClass(), VehicleSpawnData.class);
    }

    public void spawnVehicle() {
        if (level != null && !level.isClientSide) {
            UnrealZaruba.LOGGER.info("[VehicleSpawnBlockEntity] Spawning vehicle of type: " + vehicleType +
                                    " for team: " + teamColor + " at " + worldPosition);
            
            if (level instanceof ServerLevel serverLevel) {
                boolean result = ShipCreator.CreateShipFromTemplate( ResourceLocation.parse(vehicleType), worldPosition, Direction.NORTH,serverLevel, null); // TODO: Replace North with actual direction

                if (!result) {
                    UnrealZaruba.LOGGER.error("[VehicleSpawnBlockEntity] Failed to spawn vehicle of type: " + vehicleType);
                }
            } else {
                UnrealZaruba.LOGGER.error("[VehicleSpawnBlockEntity] Failed to spawn vehicle, level is not a ServerLevel");
            }
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("VehicleType", vehicleType);
        tag.putString("TeamColor", teamColor.name());
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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