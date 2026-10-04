package com.dod.unrealzaruba.mixin;

import com.dod.unrealzaruba.utils.Gamerules;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternControllerBlock.class)
public abstract class LecternControllerBlockMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    public void onUse(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (player.isShiftKeyDown() && Gamerules.DO_LINKS_SAFE && !player.mayBuild()) {
            cir.setReturnValue(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
            cir.cancel();
        }
    }
}
