package com.dod.UnrealZaruba.WorldManager;

import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Gamemodes.DestroyObjectivesGamemode;
import com.dod.UnrealZaruba.Services.LeaderboardService;
import com.dod.UnrealZaruba.WorldManager.Lobby.Lobby;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.server.ServerLifecycleHooks;

public class SimpleWorldManager {
    public static final ResourceKey<Level> GAME_DIMENSION = ResourceKey
            .create(Registries.DIMENSION, new ResourceLocation("unrealzaruba", "game_dim"));
    public static final ResourceKey<Level> LOBBY_DIMENSION = ResourceKey
            .create(Registries.DIMENSION, new ResourceLocation("unrealzaruba", "lobby_dim"));
    
    // Dimension Type - Could use the same type for all game dimensions
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey
            .create(Registries.DIMENSION_TYPE, new ResourceLocation("unrealzaruba", "custom_dimension_type"));

    public final Lobby UnrealZarubaLobby;

    public SimpleWorldManager(LeaderboardService leaderboardService) {
        UnrealZarubaLobby = new Lobby();
        var server = ServerLifecycleHooks.getCurrentServer();
        
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
