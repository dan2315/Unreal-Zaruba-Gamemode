package com.dod.UnrealZaruba;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColorArgument;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.Events.ClientEvents;
import com.dod.UnrealZaruba.Events.ServerEvents;
import com.dod.UnrealZaruba.Utils.Gamerules; // It needs to be imported
import com.dod.UnrealZaruba.Title.TitleMessage; // I fucked the NoClassDefFound
import com.dod.UnrealZaruba.Mobs.ModMobs;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.ModItems.CreativeTabs;
import com.dod.UnrealZaruba.ModItems.ModItems;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.dod.UnrealZaruba.Services.HttpClientService;
import com.dod.UnrealZaruba.Services.GameStatisticsService;
import com.dod.UnrealZaruba.UI.ModMenus;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.dod.UnrealZaruba.Events.ModSetupEvents;
import com.dod.UnrealZaruba.Vehicles.VehicleRegistry;

import com.mojang.logging.LogUtils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.dod.UnrealZaruba.Commands.CommandRegistration;
import net.minecraftforge.eventbus.api.IEventBus;

import org.slf4j.Logger;

@Mod(UnrealZaruba.MOD_ID)
public class UnrealZaruba {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "unrealzaruba";

    private static HttpClientService httpClientService;
    private static GameStatisticsService GameStatisticsService;
    public static WorldManager worldManager;

    public UnrealZaruba() {
        ConfigManager.init();
        LOGGER.info("Configuration system initialized");

        httpClientService = new HttpClientService();
        GameStatisticsService = new GameStatisticsService(httpClientService);

        TeamColorArgument.RegisterArgument();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModSounds.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModMobs.register(modEventBus);
        ModMenus.register(modEventBus);
        CreativeTabs.register(modEventBus);
        modEventBus.addListener(ModSetupEvents::onClientSetup);
        modEventBus.addListener(ModSetupEvents::registerGuiOverlays);

        CommandRegistration.init(GameStatisticsService);

        MinecraftForge.EVENT_BUS.register(ServerEvents.class);
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        MinecraftForge.EVENT_BUS.register(new NetworkHandler());
        VehicleRegistry.init();

        NetworkHandler.init();
    }
}

// TODO: 
// Spawn vehicle in right place
// Reset teams on game restart
// Reset destructible objectives on game restart
// Save skull on death
