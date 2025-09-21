package com.dod.UnrealZaruba;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColorArgument;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.Events.ClientEvents;
import com.dod.UnrealZaruba.Events.KeyBindings;
import com.dod.UnrealZaruba.Events.ServerEvents;
import com.dod.UnrealZaruba.Mobs.AttributesRegistration;
import com.dod.UnrealZaruba.Renderers.GeometryRenderer;
import com.dod.UnrealZaruba.UI.Objectives.HudObjective;
import com.dod.UnrealZaruba.Utils.Gamerules;
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
import com.dod.UnrealZaruba.Vehicles.VehicleManager;
import com.dod.UnrealZaruba.Vehicles.VehicleRegistry;

import com.mojang.logging.LogUtils;

import dlovin.advancedcompass.AdvancedCompass;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.dod.UnrealZaruba.Commands.CommandRegistration;
import com.dod.UnrealZaruba.Gamemodes.GamemodeFactory;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.GameTimer.NetworkedTimer;
import net.minecraftforge.eventbus.api.IEventBus;

import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(UnrealZaruba.MOD_ID)
public class UnrealZaruba {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "unrealzaruba";

    private static HttpClientService httpClientService;
    private static GameStatisticsService gameStatisticsService;
    public static WorldManager worldManager;
    public static VehicleManager vehicleManager;
    public static GeometryRenderer geometryRenderer;
    public static AdvancedCompass advancedCompass;

    public UnrealZaruba() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        advancedCompass = AdvancedCompass.getInstance();

        ConfigManager.init();
        MinecraftForge.EVENT_BUS.register(new NetworkHandler());
        NetworkHandler.init();
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        CreativeTabs.register(modEventBus);
        TeamColorArgument.RegisterArgument();
        ModSounds.register(modEventBus);
        ModMobs.register(modEventBus);
        ModMenus.register(modEventBus);


        if (FMLEnvironment.dist.isClient()) {
            modEventBus.register(KeyBindings.class);
        }
        if (FMLEnvironment.dist.isDedicatedServer() && !ConfigManager.getMainConfig().isZarubaServer()) {
            return;
        }

        Gamerules.DO_LINKS_SAFE = true;

        httpClientService = new HttpClientService();
        gameStatisticsService = new GameStatisticsService(httpClientService);

        modEventBus.addListener(ModSetupEvents::onClientSetup);
        modEventBus.addListener(ModSetupEvents::registerGuiOverlays);

        VehicleRegistry.init();
        vehicleManager = new VehicleManager();
        GamemodeFactory.Initialize(vehicleManager, gameStatisticsService, new NetworkedTimer());
        GamemodeManager.instance = new GamemodeManager();
        HudObjective.InitializeAllHudObjectiveTypes();

        var serverEvents = new ServerEvents(gameStatisticsService, GamemodeManager.instance);
        MinecraftForge.EVENT_BUS.register(serverEvents);

        if (FMLEnvironment.dist.isClient()) {
            var clientEvents = new ClientEvents(GamemodeManager.instance);
            MinecraftForge.EVENT_BUS.register(clientEvents);
        }

        MinecraftForge.EVENT_BUS.register(AttributesRegistration.class);

        CommandRegistration.init(gameStatisticsService);
    }
}