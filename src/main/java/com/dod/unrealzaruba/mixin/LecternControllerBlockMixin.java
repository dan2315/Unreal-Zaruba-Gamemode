package com.dod.unrealzaruba.mixin;

import com.dod.unrealzaruba.utils.Gamerules;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isShiftKeyDown() && Gamerules.DO_LINKS_SAFE && !player.mayBuild()) {
            cir.setReturnValue(InteractionResult.PASS);
            cir.cancel();
        }
    }
}
