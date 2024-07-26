package com.dod.UnrealZaruba.WorldManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.nio.file.Paths;
import java.io.FileInputStream;

public class WorldManager {
    public static String archivePath = "world_copy.zip";
    public static String worldPath = "";

    public static String temporaryMapName = "FlatLand";
    private static List<ServerPlayer> playersToReturn = new ArrayList<>();
    private static boolean reloading = false;
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void ReloadMap(MinecraftServer server) {        
        if (!server.isDedicatedServer()) {
            server.getCommands().performCommand(server.createCommandSourceStack(), "say This is a singleplayer world. Map reloading is not supported.");
            return;
        }

        // Move players to a temporary world
        movePlayersToTemporaryWorld(server);

        // Perform the map reloading process asynchronously
        CompletableFuture.runAsync(() -> {
            reloadMapFromTemplate(server);
            returnPlayersToMainWorld(server);
        }, executorService);
    }

    private static void movePlayersToTemporaryWorld(MinecraftServer server) {
        playersToReturn.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().performCommand(server.createCommandSourceStack(), "mw tp " + player.getName().getString() + " " + temporaryMapName);
            playersToReturn.add(player);
        }
    }

    private static void reloadMapFromTemplate(MinecraftServer server) {
        File destinationFolder = new File(worldPath);

        deleteFolder(destinationFolder);

        try {
            unzipArchive(archivePath, worldPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }

    private static void unzipArchive(String zipFilePath, String destDir) throws IOException {
        Path destDirPath = Paths.get(destDir);
        if (!Files.exists(destDirPath)) {
            Files.createDirectories(destDirPath);
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                Path filePath = destDirPath.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    // If the entry is a file, extracts it
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zipIn, filePath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // If the entry is a directory, make the directory
                    Files.createDirectories(filePath);
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private static void returnPlayersToMainWorld(MinecraftServer server) {
        for (ServerPlayer player : playersToReturn) {
            // Move the player back to the main world
            server.getCommands().performCommand(server.createCommandSourceStack(), "mw tp " + player.getName().getString() + " main_world");
        }
    }


    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Ensure any ongoing reloading process is properly stopped or finalized
        reloading = false;
    }
}
