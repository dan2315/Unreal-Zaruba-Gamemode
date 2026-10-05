package com.dod.unrealzaruba.ModIntegrations.Xaero;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.dod.unrealzaruba.ModIntegrations.WaypointManager;
import com.dod.unrealzaruba.NetworkPackets.MapPack.ClientboundMapPackOfferPacket;
import com.dod.unrealzaruba.NetworkPackets.MapPack.RequestMapPackPacket;
import com.dod.unrealzaruba.NetworkPackets.MapPack.UploadMapPackPacket;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Downloads the current map's pre-rendered world map pack once (cached by hash in unrealzaruba/xaero-maps/), then
 * installs it into Xaero's folder for the game dimension. Installing waits until the player is outside the game
 * dimension, because Xaero reloads a dimension's map from disk when the player enters it.
 */
public final class MapPackClient {
    private static final Pattern REGION_FILE = Pattern.compile("-?\\d+_-?\\d+\\.zip");
    private static final String COMPLETE_MARKER = ".complete";
    private static final int INSTALL_DELAY_TICKS = 60; // let Xaero finish saving the dimension the player left

    private static String offeredHash;
    private static List<ClientboundMapPackOfferPacket.FileInfo> offeredFiles = List.of();
    private static long[] received;
    private static String pendingInstall;
    private static int ticksOutsideGame;

    private MapPackClient() {}

    private static Path cacheDir(String hash) {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("unrealzaruba").resolve("xaero-maps").resolve(hash);
    }

    public static void onOffer(String mapName, String hash, List<ClientboundMapPackOfferPacket.FileInfo> files) {
        if (WaypointManager.INSTANCE.worldMap() == null || !hash.matches("[0-9a-f]{1,64}")) return;
        if (files.stream().anyMatch(file -> !REGION_FILE.matcher(file.name()).matches() || file.size() < 0)) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Ignoring map pack {} with unexpected file names", mapName);
            return;
        }
        offeredHash = hash;
        offeredFiles = files;
        Path dir = cacheDir(hash);
        if (Files.exists(dir.resolve(COMPLETE_MARKER))) {
            pendingInstall = hash;
            return;
        }
        try {
            deleteRecursively(dir);
            Files.createDirectories(dir);
            received = new long[files.size()];
            for (var file : files) {
                try (RandomAccessFile out = new RandomAccessFile(dir.resolve(file.name()).toFile(), "rw")) {
                    out.setLength(file.size());
                }
            }
            if (files.stream().allMatch(file -> file.size() == 0)) {
                complete(hash);
                return;
            }
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Downloading world map for {} ({} KB)", mapName,
                    files.stream().mapToLong(ClientboundMapPackOfferPacket.FileInfo::size).sum() / 1024);
            PacketDistributor.sendToServer(new RequestMapPackPacket(hash));
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not prepare map pack download", e);
        }
    }

    public static void onData(String hash, int fileIndex, int offset, byte[] data) {
        if (!hash.equals(offeredHash) || received == null || fileIndex < 0 || fileIndex >= offeredFiles.size()) return;
        var file = offeredFiles.get(fileIndex);
        if (offset < 0 || offset + data.length > file.size()) return;
        try (RandomAccessFile out = new RandomAccessFile(cacheDir(hash).resolve(file.name()).toFile(), "rw")) {
            out.seek(offset);
            out.write(data);
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not write map pack data", e);
            return;
        }
        received[fileIndex] += data.length;
        for (int i = 0; i < offeredFiles.size(); i++) {
            if (received[i] < offeredFiles.get(i).size()) return;
        }
        complete(hash);
    }

    private static void complete(String hash) {
        try {
            Files.writeString(cacheDir(hash).resolve(COMPLETE_MARKER), hash, StandardCharsets.UTF_8);
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not finish map pack", e);
            return;
        }
        received = null;
        pendingInstall = hash;
        UnrealZaruba.LOGGER.info("[UnrealZaruba] World map pack {} downloaded", hash);
    }

    public static void tick() {
        tickUpload();
        if (pendingInstall == null) return;
        Minecraft mc = Minecraft.getInstance();
        XaeroWorldMap worldMap = WaypointManager.INSTANCE.worldMap();
        if (mc.level == null || worldMap == null) return;
        if (mc.level.dimension().equals(XaeroWaypoints.GAME_DIMENSION)) {
            ticksOutsideGame = 0;
            return;
        }
        if (++ticksOutsideGame < INSTALL_DELAY_TICKS) return;

        List<String> names = offeredFiles.stream().map(ClientboundMapPackOfferPacket.FileInfo::name).toList();
        if (worldMap.installMapPack(cacheDir(pendingInstall), names, pendingInstall)) {
            pendingInstall = null;
        }
    }

    // ---- upload after /mapscan ----

    private static final int UPLOAD_PIECES_PER_TICK = 8;
    private static final java.util.Queue<UploadMapPackPacket> UPLOAD = new java.util.ArrayDeque<>();

    /** Sends this client's rendered game dimension regions to the server as the map's pack. */
    public static void startUpload(String mapName) {
        XaeroWorldMap worldMap = WaypointManager.INSTANCE.worldMap();
        Path folder = worldMap == null ? null : worldMap.gameDimensionFolder();
        UPLOAD.clear();
        List<Path> files = List.of();
        if (folder != null && Files.isDirectory(folder)) {
            try (Stream<Path> list = Files.list(folder)) {
                files = list.filter(path -> REGION_FILE.matcher(path.getFileName().toString()).matches()).sorted().toList();
            } catch (IOException e) {
                UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not list the rendered map", e);
            }
        }
        if (files.isEmpty()) {
            UPLOAD.add(new UploadMapPackPacket(mapName, 0, "0_0.zip", 0, 0, new byte[0]));
            return;
        }
        try {
            for (Path file : files) {
                byte[] data = Files.readAllBytes(file);
                String name = file.getFileName().toString();
                for (int offset = 0; offset < data.length || offset == 0; offset += UploadMapPackPacket.MAX_PIECE) {
                    int length = Math.min(UploadMapPackPacket.MAX_PIECE, data.length - offset);
                    byte[] piece = new byte[Math.max(0, length)];
                    System.arraycopy(data, offset, piece, 0, piece.length);
                    UPLOAD.add(new UploadMapPackPacket(mapName, files.size(), name, data.length, offset, piece));
                    if (data.length == 0) break;
                }
            }
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Uploading {} map regions for {} from {}", files.size(), mapName, folder);
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not read the rendered map", e);
            UPLOAD.clear();
        }
    }

    private static void tickUpload() {
        for (int i = 0; i < UPLOAD_PIECES_PER_TICK && !UPLOAD.isEmpty(); i++) {
            PacketDistributor.sendToServer(UPLOAD.poll());
        }
    }

    public static void reset() {
        UPLOAD.clear();
        offeredHash = null;
        offeredFiles = List.of();
        received = null;
        pendingInstall = null;
        ticksOutsideGame = 0;
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
