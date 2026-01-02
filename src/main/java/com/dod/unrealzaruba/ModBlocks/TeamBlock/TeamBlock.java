package com.dod.unrealzaruba.ModBlocks.TeamBlock;


import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.Gamemodes.TeamGamemode;
import com.dod.unrealzaruba.TeamLogic.TeamManager;
import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.network.chat.Component;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class TeamBlock extends Block {
    private final TeamColor teamColor;

    private static final Map<UUID, Long> lastMessageTimes = new HashMap<>();
    private static final long MESSAGE_COOLDOWN = 20;

    public TeamBlock(TeamColor teamColor, Properties properties) {
        super(properties);
        this.teamColor = teamColor;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState blockState, Entity entity) {
        if (!level.isClientSide && entity instanceof Player) {
            ServerPlayer player = (ServerPlayer) entity;
            if (player.isCrouching()) {
                
                BaseGamemode gamemode = GamemodeManager.instance.GetActiveGamemode();
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
            else {
                UUID playerUUID = player.getUUID();
                long currentTime = level.getGameTime();
                if (lastMessageTimes.containsKey(playerUUID) && currentTime - lastMessageTimes.get(playerUUID) < MESSAGE_COOLDOWN) {
                    return;
                }
                lastMessageTimes.put(playerUUID, currentTime);
                player.displayClientMessage(Component.translatable("block.minecraft.team_block.hint"), true);
            }
        }
    }
}
