package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.GameText.StartGameText;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public class TeamGamemode extends BaseGamemode {

    protected TeamManager TeamManager;
    public HashMap<TeamColor, StartGameText> startGameTexts = new HashMap<>();

    public TeamManager GetTeamManager() { return TeamManager; }
    public void SetTeamManager(TeamManager teamManager) { TeamManager = teamManager; }
    

    @Override
    public void HandleConnectedPlayer(Player player) {}

    @Override
    public void CheckObjectives() {}

    @Override
    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException { return 0; }


    @Override
    public void TeleportPlayersInGame(ResourceKey<Level> gameDimension) {
        // TeamManager.
    }

    // public static BaseGamemode GetCurrentGamemode() { return (TeamGamemode) currentGamemode; }
    
}
