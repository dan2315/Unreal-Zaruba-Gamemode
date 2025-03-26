package com.dod.UnrealZaruba.WorldManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Services.GameStatisticsService;
import com.dod.UnrealZaruba.WorldManager.ChunkGenerator.VoidChunkGenerator;
import com.dod.UnrealZaruba.api.IMinecraftServerExtended;

import com.mojang.serialization.Codec;
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
import org.apache.commons.lang3.tuple.Pair;

public class WorldManager {
    public static final ResourceKey<Level> GAME_DIMENSION = ResourceKey
            .create(Registries.DIMENSION, new ResourceLocation("unrealzaruba", "game_dim"));
    public static final ResourceKey<Level> LOBBY_DIMENSION = ResourceKey
            .create(Registries.DIMENSION, new ResourceLocation("unrealzaruba", "lobby_dim"));
    
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey
            .create(Registries.DIMENSION_TYPE, new ResourceLocation("unrealzaruba", "custom_dimension_type"));

    public static ServerLevel serverLevel;
    public static MinecraftServer server;
    public static LevelStorageSource.LevelStorageAccess access; 
    public static ChunkProgressListener progressListener;
    public static LevelStem levelStem;
    public static ServerLevelData serverLevelData;

    public WorldManager(GameStatisticsService leaderboardService, MinecraftServer server) {
        WorldManager.server = server;
        // ServerShipWorldCore shipWorldCore = VSGameUtilsKt.getShipObjectWorld(server.overworld());
        // LevelYRange yRange = new LevelYRange(server.overworld().getMinBuildHeight(), server.overworld().getMaxBuildHeight());
        // shipWorldCore.addDimension(GAME_DIMENSION.location().toString(), yRange);
        ResetGameWorld();
    }

    public static Pair<ResourceKey<Level>, ResourceKey<Level>> getDimensions() {
        return Pair.of(GAME_DIMENSION, LOBBY_DIMENSION);
    }

    public static void ResetGameWorld() {
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

        ((IMinecraftServerExtended) server).deleteLevel(GAME_DIMENSION);

        
        serverLevel = new ServerLevel(server, Util.backgroundExecutor(), access, serverLevelData, GAME_DIMENSION, levelStem, progressListener,false, server.getWorldData().worldGenOptions().seed(), List.of(), false, null);
        serverLevel.noSave = true;
        ((IMinecraftServerExtended) server).addLevel(GAME_DIMENSION, serverLevel);
        server.markWorldsDirty();
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

    public static void reloadWorldFromRegionFiles(MinecraftServer server, ResourceKey<Level> dimensionKey, Path regionSourcePath) {
        ServerLevel dimension = server.getLevel(dimensionKey);

        if (dimension != null) {

            Path dimensionRegionPath = Paths.get(dimensionKey.location().getPath());

            try {
                clearDirectory(dimensionRegionPath);

                // Copy the new region files
                Files.walk(regionSourcePath)
                        .filter(Files::isRegularFile)
                        .forEach(sourceFile -> {
                            Path destFile = dimensionRegionPath.resolve(regionSourcePath.relativize(sourceFile));
                            try {
                                Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                // Reload chunks or restart the server to apply changes
                // dimension.getChunkSource().; // Clear the chunk cache //TODO: Чат гупуту говорит, что кэш чанков надо очистить
                // Optionally: server.forceReload(); // Force a server reload

                System.out.println("World reloaded from region files successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Dimension not found.");
        }
    }

    private static void clearDirectory(Path path) throws IOException {
        Files.walk(path)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
