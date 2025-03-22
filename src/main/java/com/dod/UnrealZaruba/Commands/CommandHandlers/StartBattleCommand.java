package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Player.PlayerContext;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class StartBattleCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("startbattle")
                .requires(cs -> cs.hasPermission(3))
                .executes(context -> PlayerContext
                        .Get(context.getSource().getPlayerOrException().getUUID())
                        .Gamemode().StartGame(context)));
    }

    @Override
    public String getCommandName() {
        return "startbattle";
    }
} 