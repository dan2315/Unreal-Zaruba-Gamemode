package com.dod.UnrealZaruba.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    
    @Final
    private Level level;

    @Inject(at = @At("HEAD"), method = "explode")
    public void beforeExplode(CallbackInfo ci) {
        if (!level.isClientSide) ci.cancel();
    }

}
