package com.dod.UnrealZaruba.WorldManager;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.level.storage.ServerLevelData;

import java.nio.file.Path;
import java.util.Random;
import java.util.function.Function;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.mojang.serialization.Lifecycle;

import java.util.List;
import java.util.ArrayList;
import java.util.OptionalLong;

import java.io.IOException;
import java.nio.file.Files;

public class DynamicWorldManager {
        public static final ResourceKey<Level> GAME_DIMENSION = ResourceKey
                        .create(Registry.DIMENSION_REGISTRY, new ResourceLocation("gamemode"));
        public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey
                        .create(Registry.DIMENSION_TYPE_REGISTRY, GAME_DIMENSION.getRegistryName());



        public static final ResourceKey<Registry<DimensionType>> CUSTOM_DIMENSION_TYPE_REGISTRY = ResourceKey
                        .createRegistryKey(new ResourceLocation("dimension"));

        public static final ResourceKey<LevelStem> GAME_WORLD = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY,
                        new ResourceLocation("game_world"));

        public static final ChunkProgressListenerFactory factory = LoggerChunkProgressListener::new;

        private static Holder<DimensionType> dimensionTypeHolder;
        public static final DimensionType CUSTOM_DIMENSION_TYPE = DimensionType.create(
                        OptionalLong.of(10000L), // Optional fixed time (e.g., day or night)
                        true, // Has skylight
                        false, // Has ceiling
                        false, // Ultra warm (like the Nether)
                        true, // Natural dimension
                        1.0D, // Coordinate scale (default 1.0 for Overworld-like scaling)
                        false, // Create dragon fight (only true for the End)
                        false, // Piglin safe
                        true, // Beds work
                        false, // Respawn anchor works
                        false, // Has raids
                        DimensionType.MIN_Y, // Min Y level
                        DimensionType.Y_SIZE, // Height of the dimension
                        DimensionType.Y_SIZE, // Logical height
                        BlockTags.INFINIBURN_OVERWORLD, // Infiniburn tag (e.g., blocks that always burn in this
                                                        // dimension)
                        DimensionType.OVERWORLD_EFFECTS, // Effects location (use "overworld" if you want normal sky
                                                         // effects)
                        1.0F // Ambient light level (default 0.0)
        );

        public static void SetupDimensionType() {
                WritableRegistry<DimensionType> writableRegistry = new MappedRegistry<DimensionType>(
                                CUSTOM_DIMENSION_TYPE_REGISTRY, Lifecycle.experimental(),
                                (Function<DimensionType, Holder.Reference<DimensionType>>) null);
                Registry.register(writableRegistry, DIMENSION_TYPE, CUSTOM_DIMENSION_TYPE);

                dimensionTypeHolder = writableRegistry.getOrCreateHolder(DIMENSION_TYPE);
                if (dimensionTypeHolder != null) {
                        UnrealZaruba.LOGGER.warn("[ASHHHH] writable reg: " + writableRegistry);
                        UnrealZaruba.LOGGER.warn("[ASHHHH] writable reg: " + dimensionTypeHolder.value().toString());
                }
        }

        public static ServerLevel LoadWorld(MinecraftServer server, Path worldPath) throws IOException {
                // Map<ResourceKey<Level>, ServerLevel> map = server.forgeGetWorldMap();
                LevelStorageAccess storageAccess = Minecraft.getInstance().getLevelSource()
                                .createAccess(worldPath.getFileName().toString());

                if (!Files.exists(storageAccess.getLevelPath(LevelResource.ROOT))) {
                        System.err.println("The world directory does not exist: " + worldPath.toString());
                        return null;
                }

                ServerLevelData serverLevelData = server.getWorldData().overworldData();
                if (serverLevelData == null) {
                        UnrealZaruba.LOGGER.error("ServerLevelData is null. World generation might fail.");
                        return null;
                }

                ChunkGenerator chunkGenerator = WorldGenSettings.makeDefaultOverworld(server.registryAccess(),
                                (new Random()).nextLong());

                LevelStem customDimension = new LevelStem(dimensionTypeHolder, chunkGenerator);
                Registry<LevelStem> dimensionRegistry = server.registryAccess()
                                .registryOrThrow(Registry.LEVEL_STEM_REGISTRY);
                Registry.register(dimensionRegistry, "game_world", customDimension);

                List<CustomSpawner> list = new ArrayList<CustomSpawner>();
                ServerLevel world = new ServerLevel(
                                server,
                                Util.backgroundExecutor(),
                                storageAccess,
                                serverLevelData,
                                GAME_DIMENSION,
                                dimensionTypeHolder,
                                factory.create(11),
                                chunkGenerator,
                                true, // isDebug
                                228,
                                list,
                                true);

                UnrealZaruba.LOGGER.error("Forcefully loading spawn chunks.");

                var chunk = world.getChunkAt(world.getSharedSpawnPos());
                if (chunk != null) {
                        UnrealZaruba.LOGGER.error("Chunk was found");
                }

                return world;
        }
}