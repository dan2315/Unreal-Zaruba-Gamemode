package com.dod.UnrealZaruba.ModBlocks;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.unrealzaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.logging.Logger;

public class SpawnBlock extends Block {
    BlockPos Spawn = null;
    private final TeamColor teamColor;


    public SpawnBlock(TeamColor teamColor, Properties properties) {
        super(properties);
        this.teamColor = teamColor;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        TeamManager.SetSpawn(teamColor, pos.offset(0, 1, 0));
        saveBlockPos(pos);
        unrealzaruba.LOGGER.info("SAVED BLOCKPOS{}", Spawn);
    }

    private void saveBlockPos(BlockPos pos) {
        unrealzaruba.LOGGER.info("Writing BlockPos");
        Spawn = pos;
    }
}
