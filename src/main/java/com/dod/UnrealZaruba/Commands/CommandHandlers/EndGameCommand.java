package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;

public class EndGameCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(getCommandName())
                .requires(cs -> cs.hasPermission(3))
                .executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    if (level instanceof ServerLevel) {
                        GamemodeManager.Get(level.dimension()).EndGame();
                    }
                    return 0;
                }));
    }

    @Override
    public String getCommandName() {
        return "endgame";
    }
}