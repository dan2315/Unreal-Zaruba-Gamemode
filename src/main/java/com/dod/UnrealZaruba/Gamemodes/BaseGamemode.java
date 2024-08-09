package com.dod.UnrealZaruba.Gamemodes;

import java.util.HashMap;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.GameText.StartGameText;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;

public abstract class BaseGamemode {
    public static BaseGamemode currentGamemode;
    public final TeamManager TeamManager = new TeamManager();

    public GameStage gameStage = GameStage.Preparation;
    public HashMap<TeamColor, StartGameText> startGameTexts = new HashMap<>();

    public abstract void HandleConnectedPlayer(Player player);
    public abstract void CheckObjectives();
    public abstract int StartBattle (CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
}
