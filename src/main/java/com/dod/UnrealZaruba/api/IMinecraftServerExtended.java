package com.dod.UnrealZaruba.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Interface to expose additional methods for MinecraftServer
 * This interface will be implemented by the MinecraftServerMixin
 */
public interface IMinecraftServerExtended {
    
    /**
     * Adds a new level to the server's level collection.
     * 
     * @param levelKey The ResourceKey of the level to add
     * @param level The ServerLevel instance to add
     */
    void addLevel(ResourceKey<Level> levelKey, ServerLevel level);
    void deleteLevel(ResourceKey<Level> levelKey);
} 