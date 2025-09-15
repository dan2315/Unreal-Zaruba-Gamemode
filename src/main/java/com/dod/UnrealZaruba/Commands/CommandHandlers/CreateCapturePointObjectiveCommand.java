package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.Objectives.CapturePointObjective;
import com.dod.UnrealZaruba.Gamemodes.Objectives.ObjectivesData;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class CreateCapturePointObjectiveCommand implements ICommandHandler{
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("crtcpp")
        .requires(cs -> cs.hasPermission(3))
        .then(Commands.argument("name", StringArgumentType.string())
        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
        .then(Commands.argument("pos2", BlockPosArgument.blockPos())
        .executes(context -> {
            String name = StringArgumentType.getString(context, "name");
            BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(context, "pos1");
            BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(context, "pos2");

            BlockVolume volume = new BlockVolume(pos1, pos2);
            CapturePointObjective objective = new CapturePointObjective(name, volume);

            var gm = GamemodeManager.instance.GetActiveGamemode();
            var dataHandler = GamemodeDataManager.getHandler(gm.getClass(), ObjectivesData.class);
            assert dataHandler != null;
            dataHandler.addObjective(objective);
            context.getSource().sendSuccess(
                () -> Component.literal("Created objective: " + objective),
                true
            );
            return 1;
        })))));
    }

    @Override
    public String getCommandName() {
        return "crtcpp";
    }
}
