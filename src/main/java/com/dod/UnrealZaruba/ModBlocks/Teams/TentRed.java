package com.dod.UnrealZaruba.ModBlocks.Teams;

import com.dod.UnrealZaruba.ModBlocks.TentMainBlockRed;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class TentRed extends TentMainBlockRed {

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {

        super.onBlockExploded(state, level, pos, explosion);
    }
    // 6 -1

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
}
