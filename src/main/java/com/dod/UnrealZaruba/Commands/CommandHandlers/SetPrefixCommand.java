package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Utils.Utils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SetPrefixCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setprefix")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("prefix", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType
                                            .getString(context, "player");
                                    String prefix = StringArgumentType
                                            .getString(context, "prefix");

                                    ServerPlayer player = context.getSource()
                                            .getServer().getPlayerList()
                                            .getPlayerByName(playerName);
                                    if (player != null) {
                                        Utils.SetPrefixTo(player, prefix);
                                        context.getSource().sendSuccess(() ->
                                                Component.literal(
                                                        "Set prefix for " + playerName + " to " + prefix),
                                                true);
                                    } else {
                                        context.getSource()
                                                .sendFailure(Component.literal(
                                                        "Player " + playerName + " not found."));
                                    }

                                    return 1;
                                }))));
    }

    @Override
    public String getCommandName() {
        return "setprefix";
    }
} 