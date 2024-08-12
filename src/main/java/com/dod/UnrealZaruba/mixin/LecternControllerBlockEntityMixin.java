package com.dod.UnrealZaruba.mixin;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;

import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(value =  LecternControllerBlockEntity.class, remap = false)
public abstract class LecternControllerBlockEntityMixin {

    @Inject(method = "dropController", at = @At("HEAD"), cancellable = true)
    public void beforeDropController(BlockState state, CallbackInfo ci) {
        try {
            Field controllerField = LecternControllerBlockEntity.class.getDeclaredField("controller");
            if (controllerField.get(this) == null) {
                UnrealZaruba.LOGGER.warn("[Во, бля] миксин был");
                ci.cancel(); // TODO: Вместо отмены, сделать выброс пустого контроллера (для режимов, где важны ресурсы)
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
