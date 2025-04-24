package com.dod.UnrealZaruba.ModBlocks.VehicleSpawn;

import java.util.ArrayList;
import java.util.List;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;

/**
 * Data class for persisting information about vehicle spawn blocks.
 * Used by the GamemodeDataManager to save and load vehicle spawn positions.
 */
public class VehicleSpawnData {
    private List<BlockLocation> locations = new ArrayList<>();
    
    public VehicleSpawnData() {
        // Empty constructor for deserialization
    }
    
    public void addLocation(BlockPos pos, ResourceKey<Level> dimension, String vehicleType, TeamColor teamColor) {
        locations.add(new BlockLocation(pos.getX(), pos.getY(), pos.getZ(), 
                dimension.location().toString(), vehicleType, teamColor));
    }

    public boolean removeLocation(BlockPos pos, ResourceKey<Level> dimension) {
        String dimensionStr = dimension.location().toString();
        return locations.removeIf(loc -> 
            loc.x == pos.getX() && 
            loc.y == pos.getY() && 
            loc.z == pos.getZ() && 
            loc.dimension.equals(dimensionStr)
        );
    }

    public List<BlockLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<BlockLocation> locations) {
        this.locations = locations;
    }

    public boolean containsLocation(BlockPos pos, ResourceKey<Level> dimension) {
        String dimensionStr = dimension.location().toString();
        return locations.stream().anyMatch(loc -> 
            loc.x == pos.getX() && 
            loc.y == pos.getY() && 
            loc.z == pos.getZ() && 
            loc.dimension.equals(dimensionStr)
        );
    }

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