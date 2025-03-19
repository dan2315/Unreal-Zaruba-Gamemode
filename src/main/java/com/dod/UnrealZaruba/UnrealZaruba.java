package com.dod.UnrealZaruba;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColorArgument;
import com.dod.UnrealZaruba.Commands.CommandRegistration;
import com.dod.UnrealZaruba.Gamemodes.DestroyObjectivesGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.ScoreboardManager;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjectivesHandler;
import com.dod.UnrealZaruba.Mobs.ModMobs;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.ModItems.ModItems;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.RespawnCooldown.PlayerRespawnEventHandler;
import com.dod.UnrealZaruba.Services.HttpClientService;
import com.dod.UnrealZaruba.Services.LeaderboardService;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.Utils.TimerManager;
import com.dod.UnrealZaruba.WorldManager.SimpleWorldManager;
import com.mojang.logging.LogUtils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.slf4j.Logger;

@Mod(UnrealZaruba.MOD_ID)
public class UnrealZaruba {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "unrealzaruba";
    
    private static TeamGamemode gamemode;
    private static TeamManager teamManager;
    private static HttpClientService httpClientService;
    private static LeaderboardService leaderboardService;
    private static SimpleWorldManager simpleWorldManager;

    public UnrealZaruba() {
        httpClientService = new HttpClientService();
        leaderboardService = new LeaderboardService(httpClientService);
        simpleWorldManager = new SimpleWorldManager(leaderboardService);

        // Register mod event listeners
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new PlayerRespawnEventHandler());

        // Register other mod components
        TeamColorArgument.RegisterArgument();
        ModSounds.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModBlocks.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModItems.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModMobs.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Initialize network handler (register packets)
        NetworkHandler.init();
    }
    
    // ================================
    // Server-side events
    // ================================
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            TeamContext.SetupMinecraftTeams(event.getServer());
            gamemode = new DestroyObjectivesGamemode(event.getServer(), leaderboardService);
            teamManager = gamemode.GetTeamManager();
            ScoreboardManager.clearScoreboard(event.getServer());
            GamemodeManager.StartGame(event.getServer().overworld(), gamemode);
        }

        @SubscribeEvent
        public static void onServerTick(ServerTickEvent event) {
            if (event.phase == ServerTickEvent.Phase.START) {
                TimerManager.UpdateAll();
            }
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            Scoreboard scoreboard = event.getServer().getScoreboard();

            scoreboard.removePlayerTeam(TeamContext.redTeam);
            scoreboard.removePlayerTeam(TeamContext.blueTeam);

            LOGGER.info("Server has stopped. Finalizing...");
            DestructibleObjectivesHandler.Save();
            gamemode.GetTeamManager().Save();
        }

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (!server.isDedicatedServer())
                return;

            ServerPlayer player = (ServerPlayer) event.getEntity();
            PlayerContext playerContext = PlayerContext.Instantiate(player.getUUID(), player.gameMode.getGameModeForPlayer());
            playerContext.SetGamemode(gamemode);
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                playerContext.SetPreviouslyOpped();
                server.getPlayerList().deop(player.getGameProfile());
            }

            gamemode.HandleConnectedPlayer(event.getEntity());
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            LOGGER.info("COMMANDS Registered");
            CommandRegistration.onCommandRegister(event, leaderboardService);
        }
    }
    
    // ================================
    // Client-side events
    // ================================
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            LOGGER.info("COMMANDS Registered");
            CommandRegistration.onCommandRegister(event, leaderboardService);
        }
    }
}
