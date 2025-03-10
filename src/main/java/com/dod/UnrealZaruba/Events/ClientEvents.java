package com.dod.UnrealZaruba.Events;

import java.util.ArrayList;
import java.util.UUID;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Commands.CommandRegistration;
import com.dod.UnrealZaruba.UI.PlayerVoteScreen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientPlayerLoggedIn(LoggingIn event) {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        UnrealZaruba.LOGGER.info("COMMANDS Registered");
        CommandRegistration.onCommandRegister(event);
    }
}
