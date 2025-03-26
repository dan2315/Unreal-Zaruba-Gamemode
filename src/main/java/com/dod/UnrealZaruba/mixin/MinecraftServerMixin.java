package com.dod.UnrealZaruba.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.dod.UnrealZaruba.api.IMinecraftServerExtended;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements IMinecraftServerExtended {

    @Shadow
    @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;
    
    /**
     * Adds a new level to the server's level collection.
     * This method exposes the shadowed levels map to allow adding custom dimensions.
     * 
     * @param levelKey The ResourceKey of the level to add
     * @param level The ServerLevel instance to add
     */
    @Override
    public void addLevel(ResourceKey<Level> levelKey, ServerLevel level) {
        this.levels.put(levelKey, level);
    }

    @Override
    public void deleteLevel(ResourceKey<Level> levelKey) {
        this.levels.remove(levelKey);
    }
} 