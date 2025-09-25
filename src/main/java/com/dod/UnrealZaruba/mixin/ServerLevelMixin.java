package com.dod.unrealzaruba.mixin;

import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void beforeSave(ProgressListener progressListener, boolean wtf1, boolean wtf2, CallbackInfo ci) {
        if (!ConfigManager.isDevMode()) {
            ci.cancel();
        }
    }
}
