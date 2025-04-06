package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;

public class TeleportToDimensionCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tptodim")
                .executes(context -> {
                    WorldManager.teleportPlayerToDimension(
                            context.getSource().getPlayerOrException(),
                            WorldManager.GAME_DIMENSION,
                            new BlockPos(0, 100, 0));
                    return 1;
                }));
    }

    @Override
    public String getCommandName() {
        return "tptodim";
    }
} 