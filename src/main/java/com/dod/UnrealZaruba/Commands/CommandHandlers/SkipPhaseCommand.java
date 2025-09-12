package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class SkipPhaseCommand implements ICommandHandler{
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skipphase").executes(context -> {
            var gm = GamemodeManager.instance.GetActiveGamemode();
            gm.CompletePhase();
            return 1;
        }));
    }

    @Override
    public String getCommandName() {
        return "skipphase";
    }
}
