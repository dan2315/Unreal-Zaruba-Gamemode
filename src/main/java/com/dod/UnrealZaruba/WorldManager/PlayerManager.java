// package com.dod.UnrealZaruba.WorldManager;

// import net.minecraft.resources.ResourceKey;
// import net.minecraft.resources.ResourceLocation;
// import net.minecraft.core.Holder;
// import net.minecraft.core.Registry;
// import net.minecraft.server.MinecraftServer;
// import net.minecraft.server.level.ServerLevel;
// import net.minecraft.server.level.ServerPlayer;
// import net.minecraft.world.level.Level;
// import net.minecraft.world.level.biome.Biome;
// import net.minecraft.world.level.dimension.DimensionType;
// import net.minecraft.world.level.dimension.LevelStem;
// import net.minecraft.world.level.storage.LevelResource;
// import net.minecraft.world.level.storage.LevelStorageSource;
// import net.minecraftforge.server.ServerLifecycleHooks;

// import java.lang.reflect.Field;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.util.Optional;

// import com.dod.UnrealZaruba.unrealzaruba;
// import com.mojang.serialization.Lifecycle;

// public class PlayerManager {
//     public final LevelResource MINIGAMES = new LevelResource("Minigames");

//     public void teleportToLobby(ServerPlayer player) {
//         ServerLevel lobbyWorld = getLobbyWorld();
//         player.teleportTo(lobbyWorld, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
//     }

//     private ServerLevel getLobbyWorld() {
//         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
//         ResourceKey<Level> lobbyWorldKey = ResourceKey.create(Registry.DIMENSION_REGISTRY,
//                 new ResourceLocation("yourmod", "lobby"));
//         return server.getLevel(lobbyWorldKey);
//     }

//     public ServerLevel loadMinigameWorld(String minigame) {
//         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
//         Path minigameWorldPath = Paths.get(server.getWorldPath(MINIGAMES).toString(), "minigames", minigame);

//         // Check if the world folder exists
//         if (!minigameWorldPath.toFile().exists()) {
//             throw new IllegalArgumentException("Minigame world folder does not exist: " + minigameWorldPath.toString());
//         }

//         // Define the dimension key for the minigame world
//         ResourceKey<Level> minigameWorldKey = ResourceKey.create(Registry.DIMENSION_REGISTRY,
//                 new ResourceLocation(unrealzaruba.MOD_ID, minigame));
//         ServerLevel minigameWorld = server.getLevel(minigameWorldKey);

//         if (minigameWorld == null) {
//             // Load the world if not already loaded
//             minigameWorld = createAndLoadWorld(server, minigameWorldKey, minigameWorldPath);
//         }

//         return minigameWorld;
//     }

//     private ServerLevel createAndLoadWorld(MinecraftServer server, ResourceKey<Level> minigameWorldKey,
//             Path minigameWorldPath) {
//         LevelStorageSource.LevelStorageAccess levelStorageAccess;
//         try {
//             Field storageSourceField = MinecraftServer.class.getDeclaredField("storageSource");
//             storageSourceField.setAccessible(true);
//             LevelStorageSource storageSource = (LevelStorageSource) storageSourceField.get(server);
//             levelStorageAccess = storageSource.createAccess(minigameWorldPath.getFileName().toString());
//         } catch (Exception e) {
//             throw new RuntimeException("Failed to access minigame world storage", e);
//         }

//         Optional<Holder<DimensionType>> dimensionTypeHolder = server.registryAccess()
//                 .registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).getHolder(DimensionType.OVERWORLD_LOCATION);

//         if (!dimensionTypeHolder.isPresent()) {
//             throw new IllegalStateException("Unable to find dimension type for OVERWORLD_LOCATION");
//         }

//         Holder<Biome> biomeHolder = server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOrCreateHolder(
//                 ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation("minecraft:plains")));

//         LevelStem levelStem = new LevelStem(dimensionTypeHolder.get(), biomeHolder, 1.0F, 0.0F, false, false);

//         Registry<LevelStem> levelStemRegistry = server.registryAccess().registryOrThrow(Registry.LEVEL_STEM_REGISTRY);
//         levelStemRegistry.register(minigameWorldKey, levelStem, Lifecycle.stable());

//         ServerLevel minigameWorld = new ServerLevel(
//                 server,
//                 server.executor,
//                 levelStorageAccess,
//                 server.getWorldData(),
//                 minigameWorldKey,
//                 levelStem.typeHolder(),
//                 server.progressListenerFactory.create(11),
//                 false,
//                 java.util.OptionalLong.empty(),
//                 server.worldData().worldGenSettings().options());

//         server.addLevel(minigameWorld);
//         return minigameWorld;
//     }

//     public void unloadMinigameWorld(ServerLevel world) {
//         // Unload and reset the minigame world
//         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
//         server.unloadLevel(world);
//     }
// }
