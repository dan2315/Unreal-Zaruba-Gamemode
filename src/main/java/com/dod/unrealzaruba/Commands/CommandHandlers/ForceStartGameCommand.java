package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ForceStartGameCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("forcestart")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    GamemodeManager.instance.StartGame();
                    return 1;
                }));
    }

    @Override
    public String getCommandName() {
        return "forcestart";
    }
}
