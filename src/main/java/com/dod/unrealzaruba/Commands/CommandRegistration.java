package com.dod.unrealzaruba.Commands;

import com.dod.unrealzaruba.Commands.CommandHandlers.ICommandHandler;
import com.dod.unrealzaruba.Services.GameStatisticsService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.logging.Logger;

public class CommandRegistration {
    private static GameStatisticsService gameStatisticsService;
    private static final Logger LOGGER = Logger.getLogger(CommandRegistration.class.getName());

    public static void registerEventSubscribers() {
        MinecraftForge.EVENT_BUS.register(CommandRegistration.class);
    }

    @SubscribeEvent
    public static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        onCommandRegister(event);
    }

    public static void init(GameStatisticsService service) {
        gameStatisticsService = service;
        registerEventSubscribers();
    }

    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        List<ICommandHandler> commandHandlers = CommandHandlerFactory.createAllHandlers();
        
        LOGGER.info("Registering " + commandHandlers.size() + " commands");
        
        for (ICommandHandler handler : commandHandlers) {
            try {
                LOGGER.info("Registering command: " + handler.getCommandName());
                handler.register(dispatcher);
                LOGGER.info("Successfully registered command: " + handler.getCommandName());
            } catch (Exception e) {
                LOGGER.severe("Error registering command " + handler.getCommandName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        LOGGER.info("Command registration complete");
    }
}