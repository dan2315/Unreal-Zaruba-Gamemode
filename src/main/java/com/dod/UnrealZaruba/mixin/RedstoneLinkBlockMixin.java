package com.dod.UnrealZaruba.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dod.UnrealZaruba.Utils.Gamerules;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(RedstoneLinkBlock.class)
public abstract class RedstoneLinkBlockMixin {
    
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void beforeUse(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (Gamerules.DO_LINKS_SAFE && !player.mayBuild()) {
            cir.setReturnValue(InteractionResult.PASS);
            cir.cancel();
        }
    }
}
