package com.dod.UnrealZaruba.Gamemodes;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;

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
    protected void Initialize() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'Initialize'");
    }

    @Override
    public void Cleanup() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'Cleanup'");
    }

    @Override
    public void onServerTick(TickEvent.ServerTickEvent server) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onServerTick'");
    }

    @Override
    public void onPlayerTick(PlayerTickEvent event) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onPlayerTick'");
    }

    
}
