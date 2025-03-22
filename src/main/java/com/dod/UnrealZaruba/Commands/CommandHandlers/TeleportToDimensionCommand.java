package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.WorldManager.SimpleWorldManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class TeleportToDimensionCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tptodim")
                .executes(context -> {
                    SimpleWorldManager.teleportPlayerToDimension(
                            context.getSource().getPlayerOrException(),
                            SimpleWorldManager.GAME_DIMENSION);
                    return 1;
                }));
    }

    @Override
    public String getCommandName() {
        return "tptodim";
    }
} 