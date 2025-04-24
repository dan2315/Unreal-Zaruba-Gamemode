package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnDataHandler;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class TriggerVehicleSpawnsCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("triggerVehicleSpawns")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2)) // Requires operator permission
                .executes(this::triggerVehicleSpawns));
    }

    private int triggerVehicleSpawns(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        
        // Get the data handler
        VehicleSpawnDataHandler handler = GamemodeDataManager.getDataHandler(VehicleSpawnData.class, VehicleSpawnDataHandler.class);
        
        if (handler == null) {
            source.sendFailure(Component.literal("Failed to get vehicle spawn data handler"));
            return 0;
        }
        
        // Trigger all vehicle spawns
        source.sendSuccess(() -> Component.literal("Triggering all registered vehicle spawns..."), true);
        handler.triggerVehicleSpawns(server);
        
        return 1;
    }

    @Override
    public String getCommandName() {
        return "triggerVehicleSpawns";
    }
} 