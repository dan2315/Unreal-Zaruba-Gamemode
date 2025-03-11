package com.dod.UnrealZaruba.ModBlocks.TeamBlock;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.UnrealZaruba;
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
        try {
            ((TeamGamemode)GamemodeManager.Get(level)).GetTeamManager().AddTeam(teamColor, pos, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        saveBlockPos(pos);
    }

    private void saveBlockPos(BlockPos pos) {
        UnrealZaruba.LOGGER.info("Writing BlockPos");
        Spawn = pos;
    }
}
