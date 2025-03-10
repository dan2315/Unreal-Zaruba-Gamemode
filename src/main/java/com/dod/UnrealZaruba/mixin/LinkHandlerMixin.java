package com.dod.UnrealZaruba.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dod.UnrealZaruba.Utils.Gamerules;
import com.simibubi.create.content.redstone.link.LinkHandler;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;


@Mixin(value = LinkHandler.class , remap = false)
public abstract class LinkHandlerMixin {
    
    @Inject(method = "onBlockActivated",at = @At("HEAD"), cancellable = true)
    private static void beforeOnBlockActivated(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci)
    {
        if (Gamerules.DO_LINKS_SAFE && !event.getEntity().mayBuild()) {
            ci.cancel();
        }
    }
}
