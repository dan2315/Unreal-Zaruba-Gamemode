package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeData;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ListVehicleSpawnsCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("listVehicleSpawns")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2)) // Requires operator permission
                .executes(this::listVehicleSpawns));
    }

    private int listVehicleSpawns(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Get the data handler
        BaseGamemode activeGamemode = GamemodeManager.instance.GetActiveGamemode();
        VehicleSpawnData handler = GamemodeDataManager.getHandler(activeGamemode.getClass(), VehicleSpawnData.class);
        
        if (handler == null) {
            source.sendFailure(Component.literal("Failed to get vehicle spawn data handler"));
            return 0;
        }
        
        VehicleSpawnData.VehicleSpawnPayload data = handler.getData();
        if (data == null || data.getLocations().isEmpty()) {
            source.sendSuccess(() -> Component.literal("No vehicle spawn points are registered"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("Registered Vehicle Spawn Points:"), false);
        
        // List all registered spawn points
        int index = 1;
        for (VehicleSpawnData.BlockLocation location : data.getLocations()) {
            MutableComponent message = Component.literal(
                    index + ". Position: " + location.getBlockPos().toShortString() + 
                    " | Dimension: " + location.getDimensionString() + 
                    " | Type: " + location.getVehicleType() + 
                    " | Team: " + location.getTeamColor());
            
            source.sendSuccess(() -> message, false);
            index++;
        }
        
        return 1;
    }

    @Override
    public String getCommandName() {
        return "listVehicleSpawns";
    }
} 