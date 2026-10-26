package com.dod.unrealzaruba.WorldManager;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;
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
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
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

    // The map template being copied for a match, and the template the game level currently runs on (a folder name in
    // game_worlds, i.e. the map key). Read by the world commit command from other threads
    private static volatile String copyingKey;
    private static volatile String currentMapKey;

    public static ServerLevel gameLevel;
    /** Closed game levels, to see whether anything keeps them in memory (see {@link #retiredLevelsAlive()}). */
    private static final List<java.lang.ref.WeakReference<ServerLevel>> RETIRED = new ArrayList<>();
    // Between the start of a map reload and the new level being created; the old level must not count as ready
    private static volatile boolean reloading;
    private static int reloadGeneration;
    private static final long SHIP_REMOVAL_DELAY_MS = 250;
    // Chunks around spawns and objectives, loaded before players are let in. Tickets expire on their own once
    // players are there to keep the chunks loaded
    private static final TicketType<ChunkPos> PRELOAD_TICKET =
            TicketType.create("unrealzaruba_preload", Comparator.comparingLong(ChunkPos::toLong), 20 * 60);
    private static final int PRELOAD_RADIUS = 3;
    private static volatile List<ChunkPos> preloadChunks = List.of();
    public static MinecraftServer server;
    public static LevelStorageSource.LevelStorageAccess access; 
    public static ChunkProgressListener progressListener;
    public static LevelStem levelStem;
    public static ServerLevelData serverLevelData;

    public WorldManager(GameStatisticsService leaderboardService, MinecraftServer server) {
        WorldManager.server = server;
        clearShipsInDimension(gameLevel);
    }

    /** True when the game map for the current match is loaded, with the chunks players arrive in. */
    public static boolean isGameWorldReady() {
        return gameLevel != null && !reloading && preloadProgress() >= 1;
    }

    /** Terrain region files of the loaded game map, or null before a map is loaded. */
    public static java.nio.file.Path gameRegionFolder() {
        return access == null ? null : access.getDimensionPath(GAME_DIMENSION).resolve("region");
    }

    /** True while the template of this map is being copied for a match; the template must not be read or replaced then. */
    public static boolean isCopyingTemplate(String key) {
        return key != null && key.equals(copyingKey);
    }

    /** The map the game level is running on (its template folder name), or null before a map is loaded. */
    public static String currentMapKey() {
        return currentMapKey;
    }

    public static Pair<ResourceKey<Level>, ResourceKey<Level>> getDimensions() {
        return Pair.of(GAME_DIMENSION, LOBBY_DIMENSION);
    }

    public static void ReloadGameWorld(BaseGamemode gamemode) {
        var gamemodeName = gamemode.MapKey();
        deleteGameWorld();
        prepareRuntimeCopy(gamemodeName);
        prepareWorldStorage(gamemodeName);
        createGameWorld(gamemodeName);
    }

    public static void ReloadGameWorldDelayed(String gamemodeName) {
        ReloadGameWorldDelayed(gamemodeName, List::of);
    }

    /**
     * Replaces the game map: removes ships, closes the old level, copies the template (off the server thread),
     * creates the new level and loads the chunks around {@code preloadPositions}. {@link #isGameWorldReady()} turns
     * true once all of that is done, so the match can bring players in right away instead of after a fixed delay.
     */
    public static void ReloadGameWorldDelayed(String gamemodeName, Supplier<Collection<BlockPos>> preloadPositions) {
        int generation = ++reloadGeneration;
        reloading = true;
        preloadChunks = List.of();
        long start = System.currentTimeMillis();
        UnrealZaruba.LOGGER.info("Deleting ships in game world");
        clearShipsInDimension(gameLevel);

        // Sable finishes removing sub-levels on the level's next tick; close the level only after that
        TimerManager.createRealTimeTimer(SHIP_REMOVAL_DELAY_MS, () -> {
            if (generation != reloadGeneration) return; // a newer reload took over
            UnrealZaruba.LOGGER.info("Deleting game world");
            deleteGameWorld();

            // Copying a map takes a moment, so do it off the server thread
            CompletableFuture.runAsync(() -> prepareRuntimeCopy(gamemodeName)).whenComplete((ignored, error) -> server.execute(() -> {
                if (generation != reloadGeneration) return;
                if (error != null) {
                    UnrealZaruba.LOGGER.error("Failed to copy map template for {}", gamemodeName, error);
                }
                try {
                    UnrealZaruba.LOGGER.info("Creating game world");
                    prepareWorldStorage(gamemodeName);
                    createGameWorld(gamemodeName);
                    startPreload(preloadPositions.get());
                    UnrealZaruba.LOGGER.info("Game world for {} created in {} ms, loading {} chunks",
                            gamemodeName, System.currentTimeMillis() - start, preloadChunks.size());
                } finally {
                    reloading = false;
                }
            }));
        }, null);
    }

    /** Asks the level to load the chunks around these positions. They count as loaded once they're full chunks. */
    private static void startPreload(Collection<BlockPos> positions) {
        if (gameLevel == null || positions == null) return;
        List<ChunkPos> chunks = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (pos == null) continue;
            ChunkPos center = new ChunkPos(pos);
            gameLevel.getChunkSource().addRegionTicket(PRELOAD_TICKET, center, PRELOAD_RADIUS, center);
            for (int dx = -PRELOAD_RADIUS + 1; dx < PRELOAD_RADIUS; dx++) {
                for (int dz = -PRELOAD_RADIUS + 1; dz < PRELOAD_RADIUS; dz++) {
                    chunks.add(new ChunkPos(center.x + dx, center.z + dz));
                }
            }
        }
        preloadChunks = chunks;
    }

    /** How much of the preloaded area is loaded, 0..1 (1 when there's nothing to load). */
    public static float preloadProgress() {
        if (gameLevel == null || reloading) return 0;
        List<ChunkPos> chunks = preloadChunks;
        if (chunks.isEmpty()) return 1;
        int loaded = 0;
        for (ChunkPos chunk : chunks) {
            if (gameLevel.getChunkSource().hasChunk(chunk.x, chunk.z)) loaded++;
        }
        return (float) loaded / chunks.size();
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
        UnrealZaruba.LOGGER.info("Copying map template {}", gamemodeName);
        copyingKey = gamemodeName;
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
        } finally {
            copyingKey = null;
        }
    }

    /** Copies a map folder, leaving out its session.lock. */
    static void copyRecursively(Path source, Path target) throws IOException {
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
        var gamemodeName = gamemode.MapKey();
        ReloadGameWorldDelayed(gamemodeName, gamemode::GetPreloadPositions);
    }

    /**
     * The match is over and everyone is in the lobby: removes the ships and closes the game level right away, so it does not
     * keep ticking and holding its memory through the vote. The next match creates a new one.
     */
    public static void DisposeGameWorld() {
        if (gameLevel == null) return;
        int generation = ++reloadGeneration;
        UnrealZaruba.LOGGER.info("Deleting ships in game world");
        clearShipsInDimension(gameLevel);

        // Sable finishes removing sub-levels on the level's next tick; close the level only after that
        TimerManager.createRealTimeTimer(SHIP_REMOVAL_DELAY_MS, () -> {
            if (generation != reloadGeneration) return; // a reload took over
            UnrealZaruba.LOGGER.info("Disposing game world");
            deleteGameWorld();
        }, null);
    }

    private static void prepareWorldStorage(String gamemodeName) {
        currentMapKey = gamemodeName;
        LevelStorageSource levelStorageSource = LevelStorageSource.createDefault(worldsRoot().toPath());
        try {
            closeAccess();
            access = levelStorageSource.validateAndCreateAccess(gamemodeName);
        } catch (IOException | ContentValidationException e) {
            throw new RuntimeException(e);
        }

        Registry<FlatLevelGeneratorPreset> presetRegistry = server.registryAccess().registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET);
        FlatLevelGeneratorPreset voidPreset = presetRegistry.get(FlatLevelGeneratorPresets.THE_VOID);
        // A fresh copy for every level: adjustGenerationSettings() nulls the non-solid entries of the settings layer list
        // (the void is all air), so a second level on the same preset object crashed when it first generated a chunk
        FlatLevelGeneratorSettings presetSettings = voidPreset.settings();
        FlatLevelGeneratorSettings voidSettings = presetSettings.withBiomeAndLayers(
                presetSettings.getLayersInfo(), presetSettings.structureOverrides(), presetSettings.getBiome());
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
        // NeoForge ticks levels from a cached array; without this the closed level keeps ticking (Sable crashes on it)
        server.markWorldsDirty();
        ServerLevel oldLevel = gameLevel;
        gameLevel = null;

        if (oldLevel != null) {
            RETIRED.add(new java.lang.ref.WeakReference<>(oldLevel));
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
            // Mods keep closed levels alive in their own caches, ~500 MB each. Gadgets & Gizmos maps containers to their
            // level in a WeakHashMap whose values hold the keys, so those entries never go; Flywheel forgets a level on
            // Unload but caches it again while the level closes
            forgetInOtherMods(oldLevel);
            TimerManager.createRealTimeTimer(3000, () -> forgetInOtherMods(oldLevel), null);
        }
        closeAccess();
    }

    /** How many closed game levels are still in memory after a full collection: should be 0. */
    public static int retiredLevelsAlive() {
        for (int i = 0; i < 3; i++) {
            System.gc();
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        RETIRED.removeIf(reference -> reference.get() == null);
        return RETIRED.size();
    }

    private static void forgetInOtherMods(ServerLevel level) {
        forgetInFlywheel(level);
        // Create Aeronautics keeps a ticker list per level in a static map and does not drop it when the level closes
        callStatic("dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeCrystallizerManager", "clearLevel", level);
        try {
            Class<?> transformations = Class.forName("com.rieno.gadgetsandgizmos.lib.item.TimedItemTransformations", false,
                    Thread.currentThread().getContextClassLoader());
            java.lang.reflect.Field field = transformations.getDeclaredField("CONTAINERS");
            field.setAccessible(true);
            ((java.util.Map<?, ?>) field.get(null)).values().removeIf(value -> value == level);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            // other version or not installed
        } catch (ReflectiveOperationException | RuntimeException e) {
            UnrealZaruba.LOGGER.warn("Could not make Gadgets and Gizmos forget a closed level: {}", e.toString());
        }
    }

    private static void forgetInFlywheel(ServerLevel level) {
        callStatic("dev.engine_room.flywheel.lib.util.LevelAttached", "invalidateLevel", level);
    }

    /** {@code className.method(level)} when that mod is there; closed levels must not stay in other mods' static caches. */
    private static void callStatic(String className, String method, ServerLevel level) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader())
                    .getMethod(method, net.minecraft.world.level.LevelAccessor.class).invoke(null, level);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // other version or not installed
        } catch (ReflectiveOperationException | RuntimeException e) {
            UnrealZaruba.LOGGER.warn("Could not make {} forget a closed level: {}", className, e.toString());
        }
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
        // Matches run on a throwaway copy and never save. In DEV the level is the template, so edits are saved by
        // autosave and /save-all like any world
        gameLevel.noSave = !ConfigManager.isDevMode();
        ((IMinecraftServerExtended) server).addLevel(GAME_DIMENSION, gameLevel);
        server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(gameLevel));
    }

    public static void clearShipsInDimension(ServerLevel level) {
        // In DEV the game level is the template itself: its ships are part of the map being edited
        if (level == null || ConfigManager.isDevMode()) {
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
