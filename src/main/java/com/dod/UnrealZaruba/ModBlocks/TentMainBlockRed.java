package com.dod.UnrealZaruba.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;

public class TentMainBlockRed extends Block {

    public TentMainBlockRed() {
        super(BlockBehaviour.Properties.of(Material.METAL)
                .strength(2.0f, 3.0f)
                .sound(SoundType.METAL));
    }

//    @Override
//    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
////        for (Map.Entry<TeamColor, BlockPos> entry : TeamU.tent_Spawns.entrySet()) {
////            if (entry.getKey() == ) {
////                TeamU.tent_Spawns.remove(pos);
////                unrealzaruba.LOGGER.info("Удалил нахуй из tent_Spawns");
////            } else {
////                unrealzaruba.LOGGER.warn("onBlockExploded .How?{} | {}", entry.getKey().toString(), entry.getValue().toString());
////            }
////        }
//        super.onBlockExploded(state, level, pos, explosion);
//    }
//    // 6 -1
//
//    @Override
//    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
////        for (Map.Entry<TeamColor, BlockPos> entry : TeamU.tent_Spawns.entrySet()) {
////            if (entry.getKey() == BaseGamemode.currentGamemode.TeamManager.GetPlayersTeam(player).color) {
////                TeamU.tent_Spawns.remove(pos);
////            } else {
////                unrealzaruba.LOGGER.warn("onDestroyedByPlayer .How?{} | {}", entry.getKey().toString(), entry.getValue().toString());
////            }
////        }
//        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
//    }
}
