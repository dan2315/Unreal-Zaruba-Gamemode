package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Utils.Gamerules;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DoLinksSafeCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dolinkssafe")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("isSafe", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean isSafe = BoolArgumentType
                                    .getBool(context, "isSafe");

                            Gamerules.DO_LINKS_SAFE = isSafe;

                            return 1;
                        })));
    }

    @Override
    public String getCommandName() {
        return "dolinkssafe";
    }
} 