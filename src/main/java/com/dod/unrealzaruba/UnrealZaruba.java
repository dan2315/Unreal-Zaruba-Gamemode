package com.dod.unrealzaruba;

import com.dod.unrealzaruba.Camera.ClientCamera;
import com.dod.unrealzaruba.Commands.Arguments.TeamColorArgument;
import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.Events.ClientEvents;
import com.dod.unrealzaruba.Events.KeyBindings;
import com.dod.unrealzaruba.Events.ServerEvents;
import com.dod.unrealzaruba.Mobs.AttributesRegistration;
import com.dod.unrealzaruba.Renderers.GeometryRenderer;
import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import com.dod.unrealzaruba.utils.Gamerules;
import com.dod.unrealzaruba.Mobs.ModMobs;
import com.dod.unrealzaruba.ModBlocks.ModBlocks;
import com.dod.unrealzaruba.ModItems.CreativeTabs;
import com.dod.unrealzaruba.ModItems.ModItems;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.SoundHandler.ModSounds;
import com.dod.unrealzaruba.Services.HttpClientService;
import com.dod.unrealzaruba.Services.GameStatisticsService;
import com.dod.unrealzaruba.UI.ModMenus;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.dod.unrealzaruba.Events.ModSetupEvents;
import com.dod.unrealzaruba.Vehicles.VehicleManager;
import com.dod.unrealzaruba.Vehicles.VehicleRegistry;

import com.mojang.logging.LogUtils;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import com.dod.unrealzaruba.Commands.CommandRegistration;
import com.dod.unrealzaruba.Gamemodes.GamemodeFactory;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.Gamemodes.GameTimer.NetworkedTimer;
import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(UnrealZaruba.MOD_ID)
public class UnrealZaruba {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "unrealzaruba";

    private static HttpClientService httpClientService;
    private static GameStatisticsService gameStatisticsService;
    public static MinecraftServer server;
    public static WorldManager worldManager;
    public static VehicleManager vehicleManager;
    public static GeometryRenderer geometryRenderer;

    public UnrealZaruba(IEventBus modEventBus, ModContainer modContainer) {
        ConfigManager.init();
        NetworkHandler.init();
        modEventBus.addListener(NetworkHandler::onRegisterPayloads);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        CreativeTabs.register(modEventBus);
        TeamColorArgument.RegisterArgument(modEventBus);
        ModSounds.register(modEventBus);
        ModMobs.register(modEventBus);
        modEventBus.register(AttributesRegistration.class);
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

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(ModSetupEvents::onClientSetup);
            modEventBus.addListener(ModSetupEvents::registerGuiOverlays);
            modEventBus.addListener(ModSetupEvents::registerMenuScreens);
        }

        VehicleRegistry.init();
        vehicleManager = new VehicleManager();
        GamemodeFactory.Initialize(vehicleManager, gameStatisticsService, new NetworkedTimer());
        GamemodeManager.instance = new GamemodeManager();
        HudObjective.InitializeAllHudObjectiveTypes();

        var serverEvents = new ServerEvents(gameStatisticsService, GamemodeManager.instance);
        NeoForge.EVENT_BUS.register(serverEvents);

        if (FMLEnvironment.dist.isClient()) {
            var clientEvents = new ClientEvents(GamemodeManager.instance);
            NeoForge.EVENT_BUS.register(clientEvents);
            NeoForge.EVENT_BUS.register(ClientCamera.class);
        }
        CommandRegistration.init(gameStatisticsService);
    }
}