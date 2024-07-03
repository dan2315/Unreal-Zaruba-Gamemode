package com.dod.UnrealZaruba.Gamemodes;

import java.util.HashMap;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.Aaaaaaaa.StartGameText;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;

public abstract class BaseGamemode {
    public static BaseGamemode currentGamemode;
    public static final TeamManager TeamManager = new TeamManager();

    public GameStage gameStage = GameStage.Preparation;
    public HashMap<TeamColor, StartGameText> startGameTexts = new HashMap<>();

    public abstract void ProcessNewPlayer(Player player);
    public abstract int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    public abstract int StartPreparation(CommandContext<CommandSourceStack> context);
}
