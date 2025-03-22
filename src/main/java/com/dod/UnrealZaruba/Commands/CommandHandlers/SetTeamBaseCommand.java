package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColorArgument;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class SetTeamBaseCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setteambase")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("name", TeamColorArgument.color())
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
                                                                            TeamColor Team = TeamColorArgument
                                                                                    .getColor(context,
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
                                                                                    false);
                                                                            TeamManager teamManager = ((TeamGamemode) (PlayerContext
                                                                                    .Get(context.getSource()
                                                                                            .getPlayerOrException()
                                                                                            .getUUID())
                                                                                    .Gamemode()))
                                                                                    .GetTeamManager();

                                                                            teamManager
                                                                                    .Get(Team)
                                                                                    .AddBarrierVolume(
                                                                                            volume);

                                                                            context.getSource()
                                                                                    .sendSuccess(() -> Component.literal(
                                                                                                    "Created team base "
                                                                                                            + Team),
                                                                                            true);
                                                                            return 1;
                                                                        })))))))));
    }

    @Override
    public String getCommandName() {
        return "setteambase";
    }
} 