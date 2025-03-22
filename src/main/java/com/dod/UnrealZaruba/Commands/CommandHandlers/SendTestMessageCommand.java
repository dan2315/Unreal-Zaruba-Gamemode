package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SendTestMessageCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sendtestmessage")
                .requires(cs -> cs.hasPermission(3))
                .executes(context -> {
                    ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
                    serverPlayer.sendSystemMessage(Component.literal("Пока что так скоро будет"));
                    return 1;
                }));
    }

    @Override
    public String getCommandName() {
        return "sendtestmessage";
    }
} 