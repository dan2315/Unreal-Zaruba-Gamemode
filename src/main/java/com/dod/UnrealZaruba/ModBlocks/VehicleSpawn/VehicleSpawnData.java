package com.dod.UnrealZaruba.ModBlocks.VehicleSpawn;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.AbstractGamemodeData;
import com.dod.UnrealZaruba.UnrealZaruba;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;
import com.dod.UnrealZaruba.Utils.ITriggerableBlock;

/**
 * Data class for persisting information about vehicle spawn blocks.
 * Used by the GamemodeDataManager to save and load vehicle spawn positions.
 */
public class VehicleSpawnData extends AbstractGamemodeData<VehicleSpawnData.VehicleSpawnPayload> {
    private static final String DATA_NAME = "vehicleSpawns";

    public VehicleSpawnData(Class<? extends BaseGamemode> gamemodeClass) {
        super(VehicleSpawnPayload.class, gamemodeClass, DATA_NAME, new VehicleSpawnPayload());
    }
    
    @Override
    public Class<VehicleSpawnPayload> getDataClass() {
        return VehicleSpawnPayload.class;
    }

    public static class VehicleSpawnPayload {
        private List<BlockLocation> locations = new ArrayList<>();
        
        /**
         * Default constructor for serialization
         */
        public VehicleSpawnPayload() {
            // Empty constructor for GSON
        }

        public List<BlockLocation> getLocations() {
            return locations;
        }

        public void setLocations(List<BlockLocation> locations) {
            this.locations = locations != null ? locations : new ArrayList<>();
        }

        public void addLocation(BlockLocation location) {
            if (location != null) {
                locations.add(location);
            }
        }

        public BlockLocation findLocation(BlockPos pos, ResourceKey<Level> dimension) {
            String dimensionStr = dimension.location().toString();
            return locations.stream()
                .filter(loc -> 
                    loc.x == pos.getX() && 
                    loc.y == pos.getY() && 
                    loc.z == pos.getZ() && 
                    loc.dimension.equals(dimensionStr))
                .findFirst()
                .orElse(null);
        }

        public boolean containsLocation(BlockPos pos, ResourceKey<Level> dimension) {
            return findLocation(pos, dimension) != null;
        }
        
        /**
         * Remove a location
         * 
         * @param pos The block position
         * @param dimension The dimension
         * @return True if removed, false if not found
         */
        public boolean removeLocation(BlockPos pos, ResourceKey<Level> dimension) {
            String dimensionStr = dimension.location().toString();
            return locations.removeIf(loc -> 
                loc.x == pos.getX() && 
                loc.y == pos.getY() && 
                loc.z == pos.getZ() && 
                loc.dimension.equals(dimensionStr)
            );
        }
        
        /**
         * Get locations for a specific team
         * 
         * @param teamColor The team color
         * @return List of locations for that team
         */
        public List<BlockLocation> getLocationsForTeam(TeamColor teamColor) {
            return locations.stream()
                .filter(loc -> loc.teamColor == teamColor)
                .collect(Collectors.toList());
        }
        
        /**
         * Get locations for a specific vehicle type
         * 
         * @param vehicleType The vehicle type
         * @return List of locations for that vehicle type
         */
        public List<BlockLocation> getLocationsForVehicleType(String vehicleType) {
            return locations.stream()
                .filter(loc -> vehicleType.equals(loc.vehicleType))
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Add a vehicle spawn location
     * 
     * @param pos The block position
     * @param dimension The dimension
     * @param vehicleType The vehicle type
     * @param teamColor The team color
     */
    public void addLocation(BlockPos pos, ResourceKey<Level> dimension, String vehicleType, TeamColor teamColor) {
        data.addLocation(new BlockLocation(pos.getX(), pos.getY(), pos.getZ(), 
                dimension.location().toString(), vehicleType, teamColor));
        try {
            saveData();
        } catch (Exception e) {
            // Already logged in AbstractGamemodeData
        }
    }

    /**
     * Remove a vehicle spawn location
     * 
     * @param pos The block position
     * @param dimension The dimension
     * @return True if removed, false if not found
     */
    public boolean removeLocation(BlockPos pos, ResourceKey<Level> dimension) {
        boolean result = data.removeLocation(pos, dimension);
        if (result) {
            try {
                saveData();
            } catch (Exception e) {
                // Already logged in AbstractGamemodeData
            }
        }
        return result;
    }

    /**
     * Check if a location exists
     * 
     * @param pos The block position
     * @param dimension The dimension
     * @return True if the location exists, false otherwise
     */
    public boolean containsLocation(BlockPos pos, ResourceKey<Level> dimension) {
        return data.containsLocation(pos, dimension);
    }
    
    /**
     * Get a specific location
     * 
     * @param pos The block position
     * @param dimension The dimension
     * @return The location if found, null otherwise
     */
    public BlockLocation getLocation(BlockPos pos, ResourceKey<Level> dimension) {
        return data.findLocation(pos, dimension);
    }
    
    /**
     * Get all vehicle spawn locations
     * 
     * @return The list of locations
     */
    public List<BlockLocation> getLocations() {
        return data.getLocations();
    }
    
    /**
     * Get locations for a specific team
     * 
     * @param teamColor The team color
     * @return List of locations for that team
     */
    public List<BlockLocation> getLocationsForTeam(TeamColor teamColor) {
        return data.getLocationsForTeam(teamColor);
    }
    
    /**
     * Get locations for a specific vehicle type
     * 
     * @param vehicleType The vehicle type
     * @return List of locations for that vehicle type
     */
    public List<BlockLocation> getLocationsForVehicleType(String vehicleType) {
        return data.getLocationsForVehicleType(vehicleType);
    }
    
    /**
     * Trigger all vehicle spawns
     * 
     * @param server The Minecraft server
     */
    public void triggerVehicleSpawns(MinecraftServer server) {
        UnrealZaruba.LOGGER.info("[VehicleSpawnData] Triggering vehicle spawns");
        
        for (BlockLocation location : data.getLocations()) {
            ResourceKey<Level> dimensionKey = location.getDimension();
            ServerLevel level = server.getLevel(dimensionKey);
            
            if (level == null) {
                UnrealZaruba.LOGGER.warn("[VehicleSpawnData] Level not found for dimension: " + dimensionKey.location());
                continue;
            }
            
            BlockPos pos = location.getBlockPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            
            // Load the chunk if it's not already loaded
            boolean wasLoaded = level.isLoaded(pos);
            if (!wasLoaded) {
                UnrealZaruba.LOGGER.info("[VehicleSpawnData] Loading chunk at " + chunkPos + " for vehicle spawn");
                level.getChunk(chunkPos.x, chunkPos.z);
            }
            
            // Get the block and trigger it if it's a triggerable block
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            
            if (block instanceof ITriggerableBlock triggerableBlock) {
                UnrealZaruba.LOGGER.info("[VehicleSpawnData] Triggering vehicle spawn at " + pos);
                try {
                    triggerableBlock.trigger(level, pos, state);
                } catch (Exception e) {
                    UnrealZaruba.LOGGER.error("[VehicleSpawnData] Error triggering vehicle spawn at " + pos, e);
                }
            } else {
                UnrealZaruba.LOGGER.warn("[VehicleSpawnData] Block at " + pos + " is not a triggerable block");
            }
        }
    }

    /**
     * Block location data class
     */
    public static class BlockLocation {
        private int x;
        private int y;
        private int z;
        private String dimension;
        private String vehicleType;
        private TeamColor teamColor;
        
        public BlockLocation() {
            // Empty constructor for deserialization
        }
        
        public BlockLocation(int x, int y, int z, String dimension, String vehicleType, TeamColor teamColor) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.vehicleType = vehicleType;
            this.teamColor = teamColor;
        }

        public BlockLocation(BlockPos pos, ResourceKey<Level> dimension, String vehicleType, TeamColor teamColor) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.dimension = dimension.location().toString();
            this.vehicleType = vehicleType;
            this.teamColor = teamColor;
        }
        
        public BlockPos getBlockPos() {
            return new BlockPos(x, y, z);
        }
        
        public ResourceKey<Level> getDimension() {
            return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimension));
        }
        
        public String getVehicleType() {
            return vehicleType;
        }
        
        public TeamColor getTeamColor() {
            return teamColor;
        }
        
        // Getters and setters for serialization/deserialization
        
        public int getX() {
            return x;
        }
        
        public void setX(int x) {
            this.x = x;
        }
        
        public int getY() {
            return y;
        }
        
        public void setY(int y) {
            this.y = y;
        }
        
        public int getZ() {
            return z;
        }
        
        public void setZ(int z) {
            this.z = z;
        }
        
        public String getDimensionString() {
            return dimension;
        }
        
        public void setDimension(String dimension) {
            this.dimension = dimension;
        }
        
        public void setVehicleType(String vehicleType) {
            this.vehicleType = vehicleType;
        }
        
        public void setTeamColor(TeamColor teamColor) {
            this.teamColor = teamColor;
        }
    }
} 