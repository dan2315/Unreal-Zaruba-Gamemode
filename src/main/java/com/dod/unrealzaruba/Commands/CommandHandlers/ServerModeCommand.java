package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.Gamemodes.ServerMode;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** /servermode [dev|game]: shows or switches the server mode without a restart (see {@link ServerMode}). */
public class ServerModeCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("servermode")
                .requires(cs -> cs.hasPermission(3))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("The server is in " + (ConfigManager.isDevMode() ? "DEV" : "GAME") + " mode"), false);
                    return 1;
                })
                .then(Commands.literal("game").executes(context -> {
                    Component result = ServerMode.toGame(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> result, true);
                    return 1;
                }))
                .then(Commands.literal("dev").executes(context -> {
                    Component result = ServerMode.toDev(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> result, true);
                    return 1;
                })));
    }

    @Override
    public String getCommandName() {
        return "servermode";
    }
}
