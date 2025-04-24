package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnBlockEntity;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnDataHandler;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class RegisterVehicleSpawnCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("registerVehicleSpawn")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2)) // Requires operator permission
                .then(Commands.argument("position", BlockPosArgument.blockPos())
                        .then(Commands.argument("vehicleType", StringArgumentType.word())
                                .then(Commands.argument("teamColor", StringArgumentType.word())
                                        .executes(context -> {
                                            return registerVehicleSpawn(
                                                    context,
                                                    BlockPosArgument.getBlockPos(context, "position"),
                                                    StringArgumentType.getString(context, "vehicleType"),
                                                    getTeamColor(StringArgumentType.getString(context, "teamColor")));
                                        })))));
    }

    private TeamColor getTeamColor(String colorName) {
        try {
            return TeamColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default to RED if the color name is invalid
            return TeamColor.RED;
        }
    }

    private int registerVehicleSpawn(CommandContext<CommandSourceStack> context, BlockPos pos, String vehicleType, TeamColor teamColor) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        
        // Get the data handler
        VehicleSpawnDataHandler handler = GamemodeDataManager.getDataHandler(VehicleSpawnData.class, VehicleSpawnDataHandler.class);
        
        if (handler == null) {
            source.sendFailure(Component.literal("Failed to get vehicle spawn data handler"));
            return 0;
        }
        
        // Check if there's a VehicleSpawnBlockEntity at the location
        if (level.getBlockEntity(pos) instanceof VehicleSpawnBlockEntity blockEntity) {
            // Update the block entity with the vehicle type
            blockEntity.setVehicleType(vehicleType);
            
            // Register the block with the data handler
            handler.registerBlock(pos, level.dimension(), vehicleType, teamColor);
            
            source.sendSuccess(() -> Component.literal(
                    "Registered vehicle spawn at " + pos.toShortString() + 
                    " with type: " + vehicleType + 
                    " and team: " + teamColor), true);
            
            return 1;
        } else {
            // Register the block in the data handler even if there's no block entity
            handler.registerBlock(pos, level.dimension(), vehicleType, teamColor);
            
            source.sendSuccess(() -> Component.literal(
                    "Registered virtual vehicle spawn at " + pos.toShortString() + 
                    " with type: " + vehicleType + 
                    " and team: " + teamColor + 
                    " (Warning: No vehicle spawn block exists at this location)"), true);
            
            return 1;
        }
    }

    @Override
    public String getCommandName() {
        return "registerVehicleSpawn";
    }
} 