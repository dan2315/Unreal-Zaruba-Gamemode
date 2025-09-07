package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ResetGameWorldCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("resetgameworld")
                .requires(source -> source.hasPermission(4))
                .executes(context -> {
                    if (GamemodeManager.instance.GetActiveGamemode() == null) {
                        context.getSource().sendSuccess(() -> 
                            Component.literal("No gamemode is active!"), true);
                        return 0;
                    }
                    WorldManager.ReloadGameWorldDelayed(GamemodeManager.instance.GetActiveGamemode());
                    context.getSource().sendSuccess(() -> 
                        Component.literal("Game world has been reset successfully!"), true);
                    return 1;
                }));
    }

    @Override
    public String getCommandName() {
        return "resetgameworld";
    }
} 