package com.dod.UnrealZaruba.WorldManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Services.GameStatisticsService;
import com.dod.UnrealZaruba.api.IMinecraftServerExtended;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.validation.ContentValidationException;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import org.apache.commons.lang3.tuple.Pair;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.VSCore;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class WorldManager {
    public static final ResourceKey<Level> GAME_DIMENSION = ResourceKey
            .create(Registries.DIMENSION, new ResourceLocation("unrealzaruba", "game_dim"));
    public static final ResourceKey<Level> LOBBY_DIMENSION = ResourceKey
            .create(Registries.DIMENSION, new ResourceLocation("unrealzaruba", "lobby_dim"));
    
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey
            .create(Registries.DIMENSION_TYPE, new ResourceLocation("unrealzaruba", "custom_dimension_type"));

    public static ServerLevel gameLevel;
    public static MinecraftServer server;
    public static LevelStorageSource.LevelStorageAccess access; 
    public static ChunkProgressListener progressListener;
    public static LevelStem levelStem;
    public static ServerLevelData serverLevelData;

    public WorldManager(GameStatisticsService leaderboardService, MinecraftServer server) {
        WorldManager.server = server;
        prepareWorldStorage();
        ResetGameWorld();

        ShipObjectServerWorld shipObjectServerWorld;
    }

    public static Pair<ResourceKey<Level>, ResourceKey<Level>> getDimensions() {
        return Pair.of(GAME_DIMENSION, LOBBY_DIMENSION);
    }

    public static void ResetGameWorld() {
        deleteGameWorld();
        createGameWorld();
    }
    
    public static void ResetGameWorldDelayed() {
        TimerManager.createRealTimeTimer(1000 /*1s*/, () -> {
            UnrealZaruba.LOGGER.info("Deleting ships in game world");
            clearShipsInDimension(gameLevel);
            UnrealZaruba.LOGGER.info("Deleting game world");
            deleteGameWorld();
            TimerManager.createRealTimeTimer(1000 /*1s*/, () -> {
                UnrealZaruba.LOGGER.info("Creating game world");
                createGameWorld();
            }, null);
        }, null);
    }

    private static void prepareWorldStorage() {
        File file = new File("universe");
        LevelStorageSource levelStorageSource = LevelStorageSource.createDefault(file.toPath());
        try {
            if (access != null) {
                access.close();
            }
            // TODO: Manage how and why to use try with resource
            access = levelStorageSource.validateAndCreateAccess("zaruba_world");
        } catch (IOException | ContentValidationException e) {
            throw new RuntimeException(e);
        }

        Registry<FlatLevelGeneratorPreset> presetRegistry = server.registryAccess().registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET);
        FlatLevelGeneratorPreset voidPreset = presetRegistry.get(FlatLevelGeneratorPresets.THE_VOID);
        FlatLevelGeneratorSettings voidSettings = voidPreset.settings();
        ChunkGenerator chunkGenerator = new FlatLevelSource(voidSettings);

        levelStem = new LevelStem(
                server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(DIMENSION_TYPE),
                chunkGenerator
        );

        progressListener = new LoggerChunkProgressListener(11);
        serverLevelData = server.getWorldData().overworldData();
    }

    public static void deleteGameWorld() {
        ((IMinecraftServerExtended) server).deleteLevel(GAME_DIMENSION);
        gameLevel = null;
    }

    public static void createGameWorld() {
        File file = new File("universe");
        LevelStorageSource levelStorageSource = LevelStorageSource.createDefault(file.toPath());
        try {
            if (access != null) {
                access.close();
            }
            // TODO: Manage how and why to use try with resource
            access = levelStorageSource.validateAndCreateAccess("zaruba_world");
        } catch (IOException | ContentValidationException e) {
            throw new RuntimeException(e);
        }
        gameLevel = new ServerLevel(server, Util.backgroundExecutor(), access, serverLevelData, GAME_DIMENSION, levelStem, progressListener,false, server.getWorldData().worldGenOptions().seed(), List.of(), false, null);
        gameLevel.noSave = true;
        ((IMinecraftServerExtended) server).addLevel(GAME_DIMENSION, gameLevel);
        server.markWorldsDirty();
    }

    public static void clearShipsInDimension(ServerLevel level) {
        VSCore vsCore = VSGameUtilsKt.getVsCore();
        ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
        List<ServerShip> ships = new ArrayList<>(shipWorld.getAllShips());
        vsCore.deleteShips(shipWorld, ships);
    }

    public static void TeleportAllPlayersToLobby(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            teleportPlayerToDimension(player, LOBBY_DIMENSION);
        }
    }

    public static void teleportPlayerToDimension(ServerPlayer player, ResourceKey<Level> dimensionKey) {
        MinecraftServer server = player.getServer();

        if (server != null) {
            ServerLevel targetWorld = server.getLevel(dimensionKey);

            if (targetWorld != null) {
                BlockPos spawnPos = targetWorld.getSharedSpawnPos();
                player.teleportTo(targetWorld, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), player.getYRot(), player.getXRot());
            } else {
                UnrealZaruba.LOGGER.error("Dimension " + dimensionKey.location() + " is not loaded.");
            }
        }
    }
}
