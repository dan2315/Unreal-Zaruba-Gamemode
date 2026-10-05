package com.dod.unrealzaruba.WorldManager;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;

import com.dod.unrealzaruba.Services.GameStatisticsService;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.api.IMinecraftServerExtended;
import com.dod.unrealzaruba.Config.MainConfig;
import com.dod.unrealzaruba.Gamemodes.BaseGamemode;

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
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.apache.commons.lang3.tuple.Pair;
import com.dod.unrealzaruba.SableIntegration.SableShips;

public class WorldManager {
    public static final ResourceKey<Level> GAME_DIMENSION = ResourceKey
            .create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("unrealzaruba", "game_dim"));
    public static final ResourceKey<Level> LOBBY_DIMENSION = ResourceKey
            .create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("unrealzaruba", "lobby_dim"));
    
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey
            .create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("unrealzaruba", "custom_dimension_type"));

    private static final String TEMPLATE_WORLDS_DIR = "game_worlds";
    private static final String RUNTIME_WORLDS_DIR = "game_worlds_runtime";

    public static ServerLevel gameLevel;
    public static MinecraftServer server;
    public static LevelStorageSource.LevelStorageAccess access; 
    public static ChunkProgressListener progressListener;
    public static LevelStem levelStem;
    public static ServerLevelData serverLevelData;

    public WorldManager(GameStatisticsService leaderboardService, MinecraftServer server) {
        WorldManager.server = server;
        clearShipsInDimension(gameLevel);
    }

    /** Terrain region files of the loaded game map, or null before a map is loaded. */
    public static java.nio.file.Path gameRegionFolder() {
        return access == null ? null : access.getDimensionPath(GAME_DIMENSION).resolve("region");
    }

    public static Pair<ResourceKey<Level>, ResourceKey<Level>> getDimensions() {
        return Pair.of(GAME_DIMENSION, LOBBY_DIMENSION);
    }

    public static void ReloadGameWorld(BaseGamemode gamemode) {
        var gamemodeName = gamemode.getClass().getSimpleName().toLowerCase();
        deleteGameWorld();
        prepareRuntimeCopy(gamemodeName);
        prepareWorldStorage(gamemodeName);
        createGameWorld(gamemodeName);
    }

    public static void ReloadGameWorldDelayed(String gamemodeName) {
        UnrealZaruba.LOGGER.info("Deleting ships in game world");
        clearShipsInDimension(gameLevel);

        // Idunno why, but it works
        TimerManager.createRealTimeTimer(7500 /*7.5s*/, () -> {
            UnrealZaruba.LOGGER.info("Deleting game world");
            deleteGameWorld();

            // Copying a map takes a moment, so do it off the server thread
            CompletableFuture.runAsync(() -> prepareRuntimeCopy(gamemodeName)).whenComplete((ignored, error) -> server.execute(() -> {
                if (error != null) {
                    UnrealZaruba.LOGGER.error("Failed to copy map template for {}", gamemodeName, error);
                    return;
                }
                UnrealZaruba.LOGGER.info("Creating game world");
                prepareWorldStorage(gamemodeName);
                createGameWorld(gamemodeName);
            }));
        }, null);
    }

    /**
     * Maps in game_worlds/ are templates. Outside DEV mode the game level runs on a throwaway copy in
     * game_worlds_runtime/, because Minecraft writes to the loaded world in ways a mixin can't fully block
     * (entity unloads, shutdown saves, other mods). Whatever gets written then never reaches the template.
     * DEV mode loads the template directly so map edits are saved.
     */
    private static boolean useRuntimeCopy() {
        return !ConfigManager.isDevMode();
    }

    private static File worldsRoot() {
        return new File(useRuntimeCopy() ? RUNTIME_WORLDS_DIR : TEMPLATE_WORLDS_DIR);
    }

    private static void prepareRuntimeCopy(String gamemodeName) {
        if (!useRuntimeCopy()) {
            return;
        }
        Path template = Path.of(TEMPLATE_WORLDS_DIR, gamemodeName);
        Path runtime = Path.of(RUNTIME_WORLDS_DIR, gamemodeName);
        try {
            deleteRecursively(runtime);
            if (!Files.isDirectory(template)) {
                UnrealZaruba.LOGGER.error("Map template not found: {}, the game world will be empty", template.toAbsolutePath());
                return;
            }
            long start = System.currentTimeMillis();
            copyRecursively(template, runtime);
            UnrealZaruba.LOGGER.info("Copied map template {} in {} ms", template, System.currentTimeMillis() - start);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void copyRecursively(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else if (!path.getFileName().toString().equals("session.lock")) {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path p : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(p);
            }
        }
    }

    public static void ReloadGameWorldDelayed(BaseGamemode gamemode) {
        var gamemodeName = gamemode.getClass().getSimpleName().toLowerCase();
        ReloadGameWorldDelayed(gamemodeName);
    }

    private static void prepareWorldStorage(String gamemodeName) {
        LevelStorageSource levelStorageSource = LevelStorageSource.createDefault(worldsRoot().toPath());
        try {
            closeAccess();
            access = levelStorageSource.validateAndCreateAccess(gamemodeName);
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

        progressListener = LoggerChunkProgressListener.create(11);
        serverLevelData = server.getWorldData().overworldData();
    }

    // Better not try
    public static void deleteGameWorld() {
        ((IMinecraftServerExtended) server).deleteLevel(GAME_DIMENSION);
        ServerLevel oldLevel = gameLevel;
        gameLevel = null;

        if (oldLevel != null) {
            // Nobody may stay in a level that's being closed
            for (ServerPlayer player : new ArrayList<>(oldLevel.players())) {
                teleportPlayerToDimension(player, LOBBY_DIMENSION, MainConfig.getInstance().getLobbySpawnPoint());
            }
            // Close it like vanilla does on shutdown, so its chunks, threads and file handles are released.
            // Closing saves its chunks, which only matters in DEV mode; otherwise they land in the throwaway copy.
            try {
                NeoForge.EVENT_BUS.post(new LevelEvent.Unload(oldLevel));
                oldLevel.close();
            } catch (IOException e) {
                UnrealZaruba.LOGGER.error("Failed to close the old game level", e);
            }
        }
        closeAccess();
    }

    private static void closeAccess() {
        if (access == null) {
            return;
        }
        try {
            access.close();
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("Failed to close game world storage", e);
        }
        access = null;
    }

    public static void createGameWorld(String gamemodeName) {
        gameLevel = new ServerLevel(server, Util.backgroundExecutor(), access, serverLevelData, GAME_DIMENSION, levelStem, progressListener,false, server.getWorldData().worldGenOptions().seed(), List.of(), false, null);
        // boolean shouldSave = MainConfig.getInstance().getMode() == MainConfig.Mode.DEV; Maybe it will be used at some point
        gameLevel.noSave = true;
        ((IMinecraftServerExtended) server).addLevel(GAME_DIMENSION, gameLevel);
        server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(gameLevel));
    }

    public static void clearShipsInDimension(ServerLevel level) {
        if (level == null) {
            return;
        }
        SableShips.removeAll(level);
    }

    public static void TeleportAllPlayersTo(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            teleportPlayerToDimension(player, dimensionKey, MainConfig.getInstance().getLobbySpawnPoint());
        }
    }

    public static void TeleportAllPlayersTo(MinecraftServer server, ResourceKey<Level> dimensionKey, BlockPos spawnPos) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            teleportPlayerToDimension(player, dimensionKey, spawnPos);
        }
    }

    public static void teleportPlayerToDimension(ServerPlayer player, ResourceKey<Level> dimensionKey, BlockPos spawnPos) {
        MinecraftServer server = player.getServer();

        if (server != null) {
            ServerLevel targetWorld = server.getLevel(dimensionKey);

            if (targetWorld != null) {
                player.teleportTo(targetWorld, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), player.getYRot(), player.getXRot());
            } else {
                UnrealZaruba.LOGGER.error("Dimension " + dimensionKey.location() + " is not loaded.");
            }
        }
    }
}
