package com.dod.UnrealZaruba.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dod.UnrealZaruba.Utils.Gamerules;
import com.simibubi.create.content.redstone.link.LinkHandler;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@Mixin(LinkHandler.class)
public abstract class LinkHandlerMixin {
    
    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
    public void beforeOnBlockActivated(PlayerInteractEvent.RightClickBlock event, CallbackInfoReturnable<Void> cir) {
        if (Gamerules.DO_LINKS_SAFE && !event.getPlayer().mayBuild()) {
            cir.cancel();
        }
    }

}
