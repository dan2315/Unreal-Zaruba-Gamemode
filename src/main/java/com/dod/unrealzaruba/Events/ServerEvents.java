package com.dod.unrealzaruba.Events;

import com.dod.unrealzaruba.Camera.CameraShots;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.OtherModTweaks.ProtectionPixel.ArmorBalancer;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.Services.GameStatisticsService;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.dod.unrealzaruba.WorldManager.MapPacks;
import com.dod.unrealzaruba.WorldManager.MapScan;
import com.dod.unrealzaruba.Config.MainConfig;
import com.dod.unrealzaruba.Economy.Wallets;
import com.dod.unrealzaruba.Economy.SideIncome;
import com.dod.unrealzaruba.Economy.SupplyDrops;
import com.dod.unrealzaruba.Economy.SupplyCases;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsSync;

import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

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
    public void onServerTick(ServerTickEvent.Pre event) {
        MinecraftServer server = event.getServer();
        if (server == null || !server.isDedicatedServer())
            return;

        TimerManager.updateAll();
        MapPacks.tick(server);
        MapScan.tick(server);
        if (!isDevMode) {
            gamemodeManager.Tick();
            SideIncome.tick(server);
            gamemodeManager.ForGamemode(gamemode -> gamemode.onServerTick(event));
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        MinecraftServer server = event.getEntity().getServer();
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
        if (CameraShots.isCamera(player)) {
            player.setGameMode(GameType.SPECTATOR);
            return;
        }
        if (!server.isDedicatedServer())
            return;
        if (isDevMode)
            return;

        player.getInventory().clearContent();
        var activeGamemode = gamemodeManager.GetActiveGamemode();
        if (activeGamemode != null) {
            activeGamemode.HandleConnectedPlayer(event.getEntity());
            Wallets.sync(player);
            RespawnPointsSync.sendTo(player);
            MapPacks.offer(player);
            SupplyDrops.syncTo(player);
            SupplyCases.syncTo(player);
        }
        else {
            WorldManager.teleportPlayerToDimension(player, WorldManager.LOBBY_DIMENSION, MainConfig.getInstance().getLobbySpawnPoint());
            player.setGameMode(GameType.ADVENTURE);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            com.dod.unrealzaruba.Vehicles.SchematicSpawner.forget(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;


    }

    @SubscribeEvent
    public void onLivingAttacked(LivingIncomingDamageEvent event) {
        var attacker = event.getSource().getEntity();
        var victim = event.getEntity();


    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        // In 1.21 this also fires on clients (entity death packets call die()), where there's no server
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
            return;
        MinecraftServer server = serverPlayer.getServer();
        if (server == null || !server.isDedicatedServer())
            return;
        if (isDevMode)
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
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        SupplyDrops.onRightClickBlock(event);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        SupplyDrops.onBreak(event);
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        SupplyDrops.onExplosion(event);
    }

    @SubscribeEvent
    public void onBlockStateChanged(BlockStateChangedEvent event) {
        vehicleManager.OnBlockStateChanged(event);
    }
}
