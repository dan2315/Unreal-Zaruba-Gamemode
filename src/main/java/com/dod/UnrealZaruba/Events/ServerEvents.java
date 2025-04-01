package com.dod.UnrealZaruba.Events;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.DestroyObjectivesGamemode;
import com.dod.UnrealZaruba.Gamemodes.GameTimer;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.Player.TeamPlayerContext;
import com.dod.UnrealZaruba.Services.GameStatisticsService;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.dod.UnrealZaruba.Config.MainConfig;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import static com.dod.UnrealZaruba.UnrealZaruba.*;

import com.dod.UnrealZaruba.UnrealZaruba;


/**
 * Все сервер-side ивенты сюда епт
 */
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class ServerEvents {
    private static BaseGamemode gamemode;
    private static GameTimer gameTimer;
    private static GameStatisticsService GameStatisticsService;
    private static boolean isDevMode = MainConfig.getInstance().getMode() == MainConfig.Mode.DEV;


    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) return;

        gameTimer = new GameTimer(server);
        gameTimer.resetScoreboard();
        UnrealZaruba.worldManager = new WorldManager(GameStatisticsService, server);
        gamemode = new DestroyObjectivesGamemode(event.getServer(), GameStatisticsService, gameTimer);
        GamemodeManager.InitializeGamemode(WorldManager.getDimensions(), gamemode);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) return;
        if (isDevMode) return;

        if (event.phase.equals(TickEvent.Phase.START)) {
            gamemode.onServerTick(event);
            TimerManager.updateAll();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) return;
        if (isDevMode) return;

        LOGGER.info("Server has stopped. Finalizing...");
        // TODO: DestructibleObjectivesHandler.Save();
        gamemode.Cleanup();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (!server.isDedicatedServer()) return;
        if (isDevMode) return;
            
       ServerPlayer player = (ServerPlayer) event.getEntity();
       PlayerContext playerContext = TeamPlayerContext.Instantiate(player.getUUID(), player.gameMode.getGameModeForPlayer());
       playerContext.SetGamemode(gamemode);

       gamemode.HandleConnectedPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (!server.isDedicatedServer()) return;
        if (isDevMode) return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        
        BaseGamemode gamemode = GamemodeManager.Get(WorldManager.GAME_DIMENSION);
        if (gamemode == null) {
            return;
        }

        gamemode.HandleDeath(serverPlayer, event);
    }
}
