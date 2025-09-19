package com.dod.UnrealZaruba.ModBlocks.VehicleSpawn;

import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.Vehicles.Vehicle;
import com.dod.UnrealZaruba.VsIntegration.ShipCreator;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import org.apache.commons.lang3.tuple.Pair;
import java.util.List;
import org.valkyrienskies.core.api.ships.ServerShip;
import java.util.ArrayList;

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
                boolean result = ShipCreator.CreateShipFromTemplate( new ResourceLocation(vehicleType), worldPosition, Direction.NORTH,serverLevel, null); // TODO: Replace North with actual direction

                if (!result) {
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