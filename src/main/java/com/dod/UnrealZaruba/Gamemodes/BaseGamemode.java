package com.dod.UnrealZaruba.Gamemodes;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class BaseGamemode {
    protected static BaseGamemode currentGamemode;

    public GameStage gameStage = GameStage.Preparation;

    public abstract void HandleConnectedPlayer(Player player);
    public abstract void CheckObjectives();
    public abstract void TeleportPlayersInGame(ResourceKey<Level> gameDimension);
    public abstract int StartGame (CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    public void SetCurrentGamemode(BaseGamemode gamemode) {
        currentGamemode = gamemode;
    }

}
