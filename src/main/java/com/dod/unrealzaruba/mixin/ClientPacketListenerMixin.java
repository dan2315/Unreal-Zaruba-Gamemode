package com.dod.unrealzaruba.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    // The minecraft field moved to ClientCommonPacketListenerImpl in 1.20.2, so it can't be shadowed here
    @Inject(method = "handleBlockEntityData", at = @At("HEAD"), cancellable = true)
    public void handleBlockEntityData(ClientboundBlockEntityDataPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance() == null) ci.cancel();
    }
}
