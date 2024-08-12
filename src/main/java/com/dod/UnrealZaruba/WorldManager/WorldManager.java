// package com.dod.UnrealZaruba.WorldManager;

// import net.minecraft.resources.ResourceKey;
// import net.minecraft.resources.ResourceLocation;
// import net.minecraft.Util;
// import net.minecraft.client.Minecraft;
// import net.minecraft.core.Holder;
// import net.minecraft.core.Registry;
// import net.minecraft.server.MinecraftServer;
// import net.minecraft.server.level.ServerLevel;
// import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
// import net.minecraft.server.level.progress.LoggerChunkProgressListener;
// import net.minecraft.tags.BlockTags;
// import net.minecraft.world.level.CustomSpawner;
// import net.minecraft.world.level.Level;
// import net.minecraft.world.level.biome.BiomeSource;
// import net.minecraft.world.level.chunk.ChunkGenerator;
// import net.minecraft.world.level.dimension.DimensionType;
// import net.minecraft.world.level.levelgen.FlatLevelSource;
// import net.minecraft.world.level.levelgen.WorldGenSettings;
// import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
// import net.minecraft.world.level.levelgen.structure.StructureSet;
// import net.minecraft.world.level.storage.LevelResource;
// import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
// import net.minecraft.world.level.storage.ServerLevelData;

// import java.nio.file.Path;
// import java.util.Random;

// import com.google.common.base.Optional;

// import java.util.Map;

// import java.util.List;
// import java.util.ArrayList;
// import java.util.OptionalLong;

// import java.io.IOException;
// import java.nio.file.Files;

// public class WorldManager {
//     public static final ResourceKey<DimensionType> GAMEMODE_LOCATION = ResourceKey
//             .create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation("gamemode"));
//     public static final ChunkProgressListenerFactory factory = LoggerChunkProgressListener::new;

//     private static Holder<DimensionType> dimensionTypeHolder;
//     public static final DimensionType CUSTOM_DIMENSION_TYPE = DimensionType.create(
//             OptionalLong.of(10000L), // Optional fixed time (e.g., day or night)
//             true, // Has skylight
//             false, // Has ceiling
//             false, // Ultra warm (like the Nether)
//             true, // Natural dimension
//             1.0D, // Coordinate scale (default 1.0 for Overworld-like scaling)
//             false, // Create dragon fight (only true for the End)
//             false, // Piglin safe
//             true, // Beds work
//             false, // Respawn anchor works
//             false, // Has raids
//             DimensionType.MIN_Y, // Min Y level
//             DimensionType.Y_SIZE, // Height of the dimension
//             DimensionType.Y_SIZE, // Logical height
//             BlockTags.INFINIBURN_OVERWORLD, // Infiniburn tag (e.g., blocks that always burn in this dimension)
//             DimensionType.OVERWORLD_EFFECTS, // Effects location (use "overworld" if you want normal sky effects)
//             1.0F // Ambient light level (default 0.0)
//     );

//     public static void SetupDimensionType(Registry<DimensionType> registry) {

//         Registry.register(registry, GAMEMODE_LOCATION, CUSTOM_DIMENSION_TYPE);

//         dimensionTypeHolder = registry.getOrCreateHolder(GAMEMODE_LOCATION);
//     }

//     public static ServerLevel LoadWorld(MinecraftServer server, Path worldPath) throws IOException {
//         Map<ResourceKey<Level>, ServerLevel> map = server.forgeGetWorldMap();
//         LevelStorageAccess storageAccess = Minecraft.getInstance().getLevelSource()
//                 .createAccess(worldPath.getFileName().toString());

//         if (!Files.exists(storageAccess.getLevelPath(LevelResource.ROOT))) {
//             System.err.println("The world directory does not exist: " + worldPath.toString());
//             return null;
//         }

//         ServerLevelData serverLevelData = server.getWorldData().overworldData();
//         ChunkGenerator chunkgenerator = WorldGenSettings.makeDefaultOverworld(server.registryAccess(),
//                 (new Random()).nextLong());
//         ResourceKey<Level> dimensionKey = Level.OVERWORLD; // or your specific dimension key
//         List<CustomSpawner> list = new ArrayList<CustomSpawner>();
//         ServerLevel world = new ServerLevel(
//                 server,
//                 Util.backgroundExecutor(),
//                 storageAccess,
//                 serverLevelData,
//                 dimensionKey,
//                 dimensionTypeHolder,
//                 factory.create(11),
//                 chunkgenerator,
//                 true, // isDebug
//                 228,
//                 list,
//                 true);

//         return world;
//     }

//     public static ServerLevel createFlatWorld(MinecraftServer server, ResourceKey<Level> dimensionKey,
//             DimensionType dimensionType) {
        
//         // Use a simple flat world setting with no layers, which effectively creates an
//         // empty world
//         FlatLevelGeneratorSettings flatSettings = new FlatLevelGeneratorSettings();

//         // Create a FlatChunkGenerator with these settings
//         ChunkGenerator chunkGenerator = new FlatChunkGenerator(BiomeSource.of(Collections.singletonList(Biomes.PLAINS)),
//                 flatSettings);

//         // Create the ServerLevel
//         ServerLevel flatWorld = new ServerLevel(
//                 server,
//                 server.executor,
//                 server.getWorldStorage(),
//                 dimensionKey,
//                 dimensionType,
//                 chunkGenerator,
//                 server.getWorldBorder(),
//                 server.getOverworld(), // Overworld as parent
//                 server.progressListenerFactory.create(11),
//                 false, // isDebugWorld
//                 0L // seed
//         );

//         return flatWorld;
//     }
// }
