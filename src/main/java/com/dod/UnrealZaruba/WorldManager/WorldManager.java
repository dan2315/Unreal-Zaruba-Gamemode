// package com.dod.UnrealZaruba.WorldManager;

// import net.minecraft.resources.ResourceKey;
// import net.minecraft.resources.ResourceLocation;
// import net.minecraft.client.Minecraft;
// import net.minecraft.core.Holder;
// import net.minecraft.core.Registry;
// import net.minecraft.server.MinecraftServer;
// import net.minecraft.server.level.ServerLevel;
// import net.minecraft.server.level.ServerPlayer;
// import net.minecraft.world.level.Level;
// import net.minecraft.world.level.LevelSettings;
// import net.minecraft.world.level.biome.Biome;
// import net.minecraft.world.level.dimension.DimensionType;
// import net.minecraft.world.level.dimension.LevelStem;
// import net.minecraft.world.level.storage.LevelResource;
// import net.minecraft.world.level.storage.LevelStorageSource;
// import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
// import net.minecraft.world.level.storage.ServerLevelData;
// import net.minecraftforge.server.ServerLifecycleHooks;

// import java.lang.reflect.Field;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.util.Optional;
// import java.io.IOException;
// import java.nio.file.Files;

// import com.dod.UnrealZaruba.unrealzaruba;
// import com.mojang.serialization.Lifecycle;

// public class WorldManager {
//     public static ServerLevel loadWorld(MinecraftServer server, Path worldPath) throws IOException {
//         // Access the level storage (saves) to find or create the world
//         LevelStorageAccess storageAccess = Minecraft.getInstance().getLevelSource().createAccess(worldPath.getFileName().toString());

//         if (!Files.exists(storageAccess.getLevelPath(LevelResource.ROOT))) {
//             System.err.println("The world directory does not exist: " + worldPath.toString());
//             return null;
//         }

//         // Load the world settings and create the ServerLevel instance
//         ServerLevelData serverLevelData = storageAccess.getDataTag(server.registryAccess(), DimensionType.OVERWORLD_LOCATION);
//         ServerLevel world = new ServerLevel(
//                 server,
//                 server.executor,
//                 storageAccess,
//                 serverLevelData,
//                 DimensionType.OVERWORLD_LOCATION,
//                 server.registryAccess().dimensionTypes().get(DimensionType.OVERWORLD_LOCATION),
//                 server.chunkSource.chunkGenerator(),
//                 server.worldData(),
//                 null,
//                 false
//         );

//         return world;
//     }
// }
