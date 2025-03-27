package com.dod.UnrealZaruba.ModBlocks.TeamBlock;


import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;


public class TeamBlock extends Block {
    private final TeamColor teamColor;

    public TeamBlock(TeamColor teamColor, Properties properties) {
        super(properties);
        this.teamColor = teamColor;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState blockState, Entity entity) {
        if (!level.isClientSide && entity instanceof Player) {
            ServerPlayer player = (ServerPlayer) entity;
            if (player.isCrouching()) {
                UnrealZaruba.LOGGER.info("Player " + player.getName().getString() + " is crouching on team block for team " + teamColor);
                
                BaseGamemode gamemode = GamemodeManager.Get(level.dimension());
                if (gamemode == null) {
                    UnrealZaruba.LOGGER.warn("No gamemode found for level");
                    return;
                }
                
                if (!(gamemode instanceof TeamGamemode)) {
                    UnrealZaruba.LOGGER.warn("Current gamemode is not a TeamGamemode");
                    return;
                }
                
                TeamGamemode teamGamemode = (TeamGamemode) gamemode;
                TeamManager teamManager = teamGamemode.GetTeamManager();
                
                if (teamManager == null) {
                    UnrealZaruba.LOGGER.warn("No team manager found in gamemode");
                    return;
                }

                if (teamManager.IsInTeam(player)) {
                    return;
                }
                
                UnrealZaruba.LOGGER.info("Assigning player " + player.getName().getString() + " to team " + teamColor);
                teamManager.AssignToTeam(teamColor, player);
            }
        }
    }
}
