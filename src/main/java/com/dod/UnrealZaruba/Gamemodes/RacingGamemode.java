package com.dod.UnrealZaruba.Gamemodes;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class RacingGamemode extends BaseGamemode {

    @Override
    public void HandleConnectedPlayer(Player player) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'HandleConnectedPlayer'");
    }

    @Override
    public void CheckObjectives() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'CheckObjectives'");
    }

    @Override
    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'StartBattle'");
    }

    @Override
    public void TeleportPlayersInGame(ResourceKey<Level> gameDimension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'TeleportPlayersInGame'");
    }

    
}
