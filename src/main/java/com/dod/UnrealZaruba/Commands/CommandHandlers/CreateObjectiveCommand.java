package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjective;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class CreateObjectiveCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("crtobj")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("x1", IntegerArgumentType.integer())
                                .then(Commands.argument("y1",
                                                IntegerArgumentType.integer())
                                        .then(Commands.argument("z1",
                                                        IntegerArgumentType
                                                                .integer())
                                                .then(Commands.argument(
                                                                "x2",
                                                                IntegerArgumentType
                                                                        .integer())
                                                        .then(Commands.argument(
                                                                        "y2",
                                                                        IntegerArgumentType
                                                                                .integer())
                                                                .then(Commands
                                                                        .argument("z2", IntegerArgumentType
                                                                                .integer())
                                                                        .executes(context -> {
                                                                            String name = StringArgumentType
                                                                                    .getString(context,
                                                                                            "name");
                                                                            int x1 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "x1");
                                                                            int y1 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "y1");
                                                                            int z1 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "z1");
                                                                            int x2 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "x2");
                                                                            int y2 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "y2");
                                                                            int z2 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "z2");

                                                                            BlockVolume volume = new BlockVolume(
                                                                                    new BlockPos(x1, y1,
                                                                                            z1),
                                                                                    new BlockPos(x2, y2,
                                                                                            z2),
                                                                                    true);

                                                                            DestructibleObjective objective = new DestructibleObjective(
                                                                                    volume,
                                                                                    name);
                                                                        // TODO:     DestructibleObjectivesHandler
                                                                        //             .Add(objective);

                                                                            context.getSource()
                                                                                    .sendSuccess(() -> Component.literal(
                                                                                                    "Created objective: "
                                                                                                            + objective),
                                                                                            true);
                                                                            return 1;
                                                                        })))))))));
    }

    @Override
    public String getCommandName() {
        return "crtobj";
    }
} 