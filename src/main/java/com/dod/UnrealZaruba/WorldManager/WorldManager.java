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

public class WorldManager {
    public static String pathToMapTemplate = "";
    public static String pathToRoot = "";

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
        File templateFolder = new File(pathToMapTemplate);
        File destinationFolder = new File(pathToRoot);

        deleteFolder(destinationFolder);

        try {
            copyFolder(templateFolder, destinationFolder);
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

    private static void copyFolder(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdir();
            }
            String[] children = source.list();
            for (String child : children) {
                copyFolder(new File(source, child), new File(destination, child));
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
