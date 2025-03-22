package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public interface ICommandHandler {

    void register(CommandDispatcher<CommandSourceStack> dispatcher);
    
    String getCommandName();
} 