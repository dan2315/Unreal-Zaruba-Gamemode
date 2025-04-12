package com.dod.UnrealZaruba.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.api.IMinecraftServerExtended;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements IMinecraftServerExtended {

    @Shadow
    @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;
    
    @Override
    public void addLevel(ResourceKey<Level> levelKey, ServerLevel level) {
        this.levels.put(levelKey, level);
    }

    @Override
    public void deleteLevel(ResourceKey<Level> levelKey) {
        this.levels.remove(levelKey);
    }

    @Inject(method = "saveAllChunks", at = @At("HEAD"), cancellable = true)
    public void saveAllLevels(CallbackInfoReturnable<Boolean> ci) {
        if (!ConfigManager.isDevMode()) {
            ci.setReturnValue(false);
            ci.cancel();
        }
    }
} 