package com.dod.UnrealZaruba.mixin;

import com.dod.UnrealZaruba.ModItems.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class)
public abstract class mayUseItemAtMixin {

    @Inject(method = "mayUseItemAt",at = @At("HEAD"), cancellable = true)
    public void BeforeMayUseItemAt(BlockPos p_36205_, Direction p_36206_, ItemStack p_36207_, CallbackInfoReturnable<Boolean> cir) {
        if (p_36207_.is(ModItems.TENT.get())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
