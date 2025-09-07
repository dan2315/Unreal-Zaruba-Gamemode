package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Gamemodes.ShipsGamemode;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class RemoveVehicleSpawnCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("removeVehicleSpawn")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2)) // Requires operator permission
                .then(Commands.argument("position", BlockPosArgument.blockPos())
                        .executes(context -> {
                            return removeVehicleSpawn(
                                    context,
                                    BlockPosArgument.getBlockPos(context, "position"));
                        })));
    }

    private int removeVehicleSpawn(CommandContext<CommandSourceStack> context, BlockPos pos) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        
        // Get the data handler
        VehicleSpawnData handler = GamemodeDataManager.getHandler(ShipsGamemode.class, VehicleSpawnData.class);
        VehicleSpawnData.VehicleSpawnPayload data = handler.getData();
        if (data == null) {
            source.sendFailure(Component.literal("Failed to get vehicle spawn data handler"));
            return 0;
        }
        
        // Check if the location is registered
        if (!data.containsLocation(pos, level.dimension())) {
            source.sendFailure(Component.literal("No vehicle spawn registered at " + pos.toShortString()));
            return 0;
        }
        
        // Unregister the block
        data.removeLocation(pos, level.dimension());
        
        source.sendSuccess(() -> Component.literal(
                "Removed vehicle spawn at " + pos.toShortString()), true);
        
        return 1;
    }

    @Override
    public String getCommandName() {
        return "removeVehicleSpawn";
    }
} 