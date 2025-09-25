package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.Gamemodes.TeamGamemode;
import com.dod.unrealzaruba.TeamLogic.TeamManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to add team data for the active gamemode.
 * Usage: /addteamdata <color>
 * Example: /addteamdata RED
 */
public class AddTeamDataCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("addteamdata")
                .requires(cs -> cs.hasPermission(3)) // Admin level permission
                .then(Commands.argument("color", StringArgumentType.word())
                        .executes(context -> {
                            String teamColorStr = StringArgumentType.getString(context, "color");
                            TeamColor teamColor = TeamColor.fromString(teamColorStr);
                            
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            BlockPos playerPos = player.blockPosition();
                            
                            // Get active gamemode
                            BaseGamemode activeGamemode = GamemodeManager.instance.GetActiveGamemode();
                            
                            if (activeGamemode == null) {
                                context.getSource().sendFailure(Component.literal("No active gamemode. Set a gamemode first."));
                                return 0;
                            }
                            
                            // Check if the active gamemode is a TeamGamemode
                            if (!(activeGamemode instanceof TeamGamemode)) {
                                context.getSource().sendFailure(Component.literal("Active gamemode doesn't support teams."));
                                return 0;
                            }
                            
                            // Get team manager from the active gamemode
                            TeamManager teamManager = ((TeamGamemode) activeGamemode).GetTeamManager();
                            
                            if (teamManager == null) {
                                context.getSource().sendFailure(Component.literal("Team manager not initialized."));
                                return 0;
                            }
                            
                            // Add team data using player's current position as spawn point
                            teamManager.AddTeam(teamColor, playerPos);
                            
                            // Save the team data
                            teamManager.Save();
                            
                            context.getSource().sendSuccess(
                                () -> Component.literal(
                                    "Added " + teamColor.getDisplayName() + " team data at position " +
                                    playerPos.getX() + ", " + playerPos.getY() + ", " + playerPos.getZ()
                                ),
                                true
                            );
                            
                            return 1;
                        })));
    }

    @Override
    public String getCommandName() {
        return "addteamdata";
    }
}
