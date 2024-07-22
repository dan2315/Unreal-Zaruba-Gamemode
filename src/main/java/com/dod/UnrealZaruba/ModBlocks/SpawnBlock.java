package com.dod.UnrealZaruba.ModBlocks;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.unrealzaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;


public class SpawnBlock extends Block {
    BlockPos Spawn = null;
    private final TeamColor teamColor;


    public SpawnBlock(TeamColor teamColor, Properties properties) {
        super(properties);
        this.teamColor = teamColor;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        BaseGamemode.currentGamemode.TeamManager.AddTeam(teamColor, pos);
        saveBlockPos(pos);
    }

    private void saveBlockPos(BlockPos pos) {
        unrealzaruba.LOGGER.info("Writing BlockPos");
        Spawn = pos;
    }
}
