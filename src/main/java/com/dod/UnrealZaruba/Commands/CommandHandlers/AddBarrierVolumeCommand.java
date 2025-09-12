package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Gamemodes.Barriers.BarrierVolumesData;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class AddBarrierVolumeCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("addbarriervolume")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                .executes(context -> {
                    BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(context, "pos1");
                    BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(context, "pos2");

                    BlockVolume volume = new BlockVolume(pos1, pos2);

                    var gm = GamemodeManager.instance.GetActiveGamemode();
                    var dataHandler = GamemodeDataManager.getHandler(gm.getClass(), BarrierVolumesData.class);
                    assert dataHandler != null;
                    dataHandler.AddBarrier(volume);

                    context.getSource().sendSuccess(() -> Component.literal("Added barrier volume"),true);
                    return 1;
        }))));
    }

    @Override
    public String getCommandName() {
        return "addbarriervolume";
    }
} 