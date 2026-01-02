package com.dod.unrealzaruba.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public class ArmorStandMixin {
    @Inject(method = "swapItem", at = @At("HEAD"), cancellable = true)
    public void beforeSwapItem(Player player, EquipmentSlot equipmentSlot, ItemStack itemStack, InteractionHand interactionHand, CallbackInfoReturnable<Boolean> info) {
        if (!player.mayBuild()) {
            info.setReturnValue(false);
        }
    }
}
