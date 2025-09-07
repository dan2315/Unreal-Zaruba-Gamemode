package com.dod.UnrealZaruba.ModBlocks.TeamBlock;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.dod.UnrealZaruba.TeamLogic.TeamData;

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
            ((TeamGamemode)GamemodeManager.instance.GetActiveGamemode()).GetTeamManager().AddTeam(teamColor, pos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        saveBlockPos(pos);
    }

    private void saveBlockPos(BlockPos pos) {
        UnrealZaruba.LOGGER.info("Writing BlockPos");
        BaseGamemode activeGamemode = GamemodeManager.instance.GetActiveGamemode();
        var handler = GamemodeDataManager.getHandler(activeGamemode.getClass(), TeamData.class);
        var data =handler.getData();
        data.getTeams().get(teamColor).setBlockPos(pos);
        handler.setData(data);
    }
}
