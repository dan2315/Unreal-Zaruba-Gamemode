package com.dod.UnrealZaruba.ModBlocks.VehicleSpawn;

import java.io.IOException;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.ITriggerableBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Handler for vehicle spawn data, managing the registration, saving, and loading of data.
 */
public class VehicleSpawnDataHandler {
    private static final String DATA_NAME = "vehicleSpawns";
    private VehicleSpawnData data;
    private final Class<? extends BaseGamemode> gamemodeClass;
    
    public VehicleSpawnDataHandler(Class<? extends BaseGamemode> gamemodeClass) {
        this.gamemodeClass = gamemodeClass;
        this.data = new VehicleSpawnData();
    }

    public void registerBlock(BlockPos pos, ResourceKey<Level> dimension, String vehicleType, TeamColor teamColor) {
        if (!data.containsLocation(pos, dimension)) {
            data.addLocation(pos, dimension, vehicleType, teamColor);
            saveData();
        }
    }

    public void unregisterBlock(BlockPos pos, ResourceKey<Level> dimension) {
        if (data.removeLocation(pos, dimension)) {
            saveData();
        }
    }

    public void saveData() {
        try {
            GamemodeDataManager.saveData(gamemodeClass, DATA_NAME, data);
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[VehicleSpawnDataHandler] Failed to save vehicle spawn data", e);
        }
    }

    public VehicleSpawnData loadData() {
        VehicleSpawnData loadedData = GamemodeDataManager.loadData(gamemodeClass, DATA_NAME, VehicleSpawnData.class);
        if (loadedData != null) {
            this.data = loadedData;
            return loadedData;
        } else {
            this.data = new VehicleSpawnData();
            return this.data;
        }
    }

    public VehicleSpawnData getData() {
        return data;
    }

    public void triggerVehicleSpawns(MinecraftServer server) {
        UnrealZaruba.LOGGER.info("[VehicleSpawnDataHandler] Triggering vehicle spawns");
        
        for (VehicleSpawnData.BlockLocation location : data.getLocations()) {
            ResourceKey<Level> dimensionKey = location.getDimension();
            ServerLevel level = server.getLevel(dimensionKey);
            
            if (level == null) {
                UnrealZaruba.LOGGER.warn("[VehicleSpawnDataHandler] Level not found for dimension: " + dimensionKey.location());
                continue;
            }
            
            BlockPos pos = location.getBlockPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            
            // Load the chunk if it's not already loaded
            boolean wasLoaded = level.isLoaded(pos);
            if (!wasLoaded) {
                UnrealZaruba.LOGGER.info("[VehicleSpawnDataHandler] Loading chunk at " + chunkPos + " for vehicle spawn");
                level.getChunk(chunkPos.x, chunkPos.z);
            }
            
            // Get the block and trigger it if it's a triggerable block
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            
            if (block instanceof ITriggerableBlock triggerableBlock) {
                UnrealZaruba.LOGGER.info("[VehicleSpawnDataHandler] Triggering vehicle spawn at " + pos);
                try {
                    triggerableBlock.trigger(level, pos, state);
                } catch (Exception e) {
                    UnrealZaruba.LOGGER.error("[VehicleSpawnDataHandler] Error triggering vehicle spawn at " + pos, e);
                }
            } else {
                UnrealZaruba.LOGGER.warn("[VehicleSpawnDataHandler] Block at " + pos + " is not a triggerable block");
            }
        }
    }
} 