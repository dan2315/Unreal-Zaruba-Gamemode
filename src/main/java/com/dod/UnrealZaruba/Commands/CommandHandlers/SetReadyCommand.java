package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Player.PlayerContext;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;


public class SetReadyCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(getCommandName())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        context.getSource().sendFailure(Component.literal("You must be a player to use this command"));
                        return 0;
                    }
                    
                    PlayerContext playerContext = PlayerContext.Get(player.getUUID());
                    playerContext.SetReady(!playerContext.IsReady());
                    context.getSource().sendSuccess(() -> Component.literal("Set ready to " + playerContext.IsReady()), true);
                    return 1;
                }));
    }

    @Override
    public String getCommandName() {
        return "ready";
    }
}
