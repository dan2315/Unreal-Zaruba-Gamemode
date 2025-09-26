package com.dod.unrealzaruba.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dod.unrealzaruba.ModItems.ModItems;

import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow
    @Final
    Item item;

    @Inject(method = "hasAdventureModePlaceTagForBlock", at =  @At("HEAD"), cancellable = true) 
    public void beforeHasAdventureModePlaceTagForBlock(Registry<Block> p_204122_, BlockInWorld p_204123_, CallbackInfoReturnable<Boolean> cir) {
        if (item == ModItems.TENT.get()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
