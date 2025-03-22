package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.CommanderSystem.CommanderSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class VoteCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vote")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(0))
                .then(Commands.argument("playerr", EntityArgument.player())
                        .executes(context -> {
                            voteForPlayer(context,
                                    EntityArgument.getPlayer(context, "playerr"));
                            return 1;
                        })));
    }

    private int voteForPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player)
            throws CommandSyntaxException {
        CommanderSystem.ProcessCommanderVote(context.getSource().getPlayerOrException().getUUID(), player.getUUID());
        return 1;
    }

    @Override
    public String getCommandName() {
        return "vote";
    }
} 