package com.dod.unrealzaruba.Events;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.OtherModTweaks.ProtectionPixel.ArmorBalancer;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.Services.GameStatisticsService;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.dod.unrealzaruba.Config.MainConfig;

import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import static com.dod.unrealzaruba.UnrealZaruba.vehicleManager;

public class ServerEvents {
    private GamemodeManager gamemodeManager;
    private GameStatisticsService GameStatisticsService;
    private static boolean isDevMode = MainConfig.getInstance().getMode() == MainConfig.Mode.DEV;

    public ServerEvents(GameStatisticsService gameStatisticsService, GamemodeManager gamemodeManager) {
        GameStatisticsService = gameStatisticsService;
        this.gamemodeManager = gamemodeManager;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        UnrealZaruba.server = event.getServer();
        if (!UnrealZaruba.server.isDedicatedServer())
            return;

        UnrealZaruba.worldManager = new WorldManager(GameStatisticsService, UnrealZaruba.server);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (server == null || !server.isDedicatedServer())
            return;

        if (event.phase.equals(TickEvent.Phase.START)) {
            TimerManager.updateAll();
            if (!isDevMode) {
                gamemodeManager.Tick();
                gamemodeManager.ForGamemode(gamemode -> gamemode.onServerTick(event));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        MinecraftServer server = event.player.getServer();
        if (server == null || !server.isDedicatedServer()) return;

        if (!isDevMode) gamemodeManager.ForGamemode(gamemode -> gamemode.onPlayerTick(event));
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer())
            return;
        if (isDevMode)
            return;

        UnrealZaruba.LOGGER.info("Server has stopped. Finalizing...");
        gamemodeManager.ForGamemode(gamemode -> gamemode.Cleanup());
    }


    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = (ServerPlayer) event.getEntity();
        PlayerContext playerContext = TeamPlayerContext.Instantiate(player.getUUID(), player.gameMode.getGameModeForPlayer());
        if (!server.isDedicatedServer())
            return;
        if (isDevMode)
            return;

        var activeGamemode = gamemodeManager.GetActiveGamemode();
        if (activeGamemode != null) {
            activeGamemode.HandleConnectedPlayer(event.getEntity());
        }
        else {
            WorldManager.teleportPlayerToDimension(player, WorldManager.LOBBY_DIMENSION, MainConfig.getInstance().getLobbySpawnPoint());
            player.setGameMode(GameType.ADVENTURE);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;


    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (!server.isDedicatedServer())
            return;
        if (isDevMode)
            return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
            return;

        gamemodeManager.ForGamemode(gamemode -> gamemode.HandleDeath(serverPlayer, event));
    }

    @SubscribeEvent
    public void onItemSpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            itemEntity.lifespan = 200;
        }
    }

    @SubscribeEvent
    public void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ArmorBalancer.onItemAttributeModifier(event);
    }

    @SubscribeEvent
    public void onBlockStateChanged(BlockStateChangedEvent event) {
        vehicleManager.OnBlockStateChanged(event);
    }
}
