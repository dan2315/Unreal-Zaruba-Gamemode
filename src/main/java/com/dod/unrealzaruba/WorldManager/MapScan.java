package com.dod.unrealzaruba.WorldManager;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.dod.unrealzaruba.Config.MainConfig;
import com.dod.unrealzaruba.NetworkPackets.MapPack.ClientboundMapPackUploadRequestPacket;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * /mapscan: flies an op over every part of the loaded map that has terrain so their Xaero's World Map renders all of
 * it, then sends them back to the lobby (Xaero saves the map when leaving a dimension) and has their client upload
 * the rendered regions as the map's pack for everyone.
 */
public final class MapScan {
    private static final Pattern REGION_FILE = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    // Sable keeps ship plots in regions far away from the map; those are not terrain to show
    private static final int MAX_REGION_COORD = 1000;
    // Saved void chunks compress to a few hundred bytes, chunks with terrain to a few KB
    private static final int MIN_TERRAIN_BYTES = 1000;
    private static final int SCAN_Y = 140;
    private static final int UPLOAD_DELAY_TICKS = 100;

    private record Scan(UUID player, String mapName, List<ChunkPos> stops, int waitTicks, GameType originalMode) {}

    private static Scan scan;
    private static int stopIndex;
    private static int ticksLeft;
    private static boolean returning;
    private static UUID expectedUploader;
    private static String expectedUploadMap;

    private MapScan() {}

    public static Component start(ServerPlayer player, String mapName, int waitSeconds) {
        if (scan != null) return Component.literal("A map scan is already running");
        Path regions = WorldManager.gameRegionFolder();
        if (WorldManager.gameLevel == null || regions == null || !Files.isDirectory(regions)) {
            return Component.literal("No game map is loaded");
        }

        int viewDistance = Math.max(2, player.server.getPlayerList().getViewDistance());
        // Each stop shows a square of (2 * viewDistance + 1) chunks; overlap by one chunk on every side
        int step = Math.max(1, 2 * viewDistance - 1);
        List<ChunkPos> stops = planStops(terrainChunks(regions), step);
        if (stops.isEmpty()) return Component.literal("The map has no terrain to scan");

        scan = new Scan(player.getUUID(), mapName, stops, waitSeconds * 20, player.gameMode.getGameModeForPlayer());
        stopIndex = 0;
        ticksLeft = 0;
        returning = false;
        player.setGameMode(GameType.SPECTATOR);
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Map scan of {}: {} stops, view distance {}", mapName, stops.size(), viewDistance);
        return Component.literal("Scanning " + mapName + ": " + stops.size() + " stops, about "
                + (stops.size() * waitSeconds) + " s. Keep your render distance at " + viewDistance + " or more.");
    }

    public static void tick(MinecraftServer server) {
        if (scan == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(scan.player());
        if (player == null || WorldManager.gameLevel == null) {
            scan = null;
            return;
        }
        if (ticksLeft-- > 0) return;

        if (returning) {
            // Back in the lobby long enough for Xaero to have saved the game dimension: ask for the files
            expectedUploader = scan.player();
            expectedUploadMap = scan.mapName();
            PacketDistributor.sendToPlayer(player, new ClientboundMapPackUploadRequestPacket(scan.mapName()));
            player.sendSystemMessage(Component.literal("Uploading the rendered map..."));
            scan = null;
            return;
        }

        if (stopIndex < scan.stops().size()) {
            ChunkPos stop = scan.stops().get(stopIndex++);
            player.teleportTo(WorldManager.gameLevel, stop.getMiddleBlockX() + 0.5, SCAN_Y, stop.getMiddleBlockZ() + 0.5, 0, 90);
            player.displayClientMessage(Component.literal("Map scan " + stopIndex + "/" + scan.stops().size()), true);
            ticksLeft = scan.waitTicks();
            return;
        }

        WorldManager.teleportPlayerToDimension(player, WorldManager.LOBBY_DIMENSION, MainConfig.getInstance().getLobbySpawnPoint());
        player.setGameMode(scan.originalMode());
        returning = true;
        ticksLeft = UPLOAD_DELAY_TICKS;
    }

    /** Whether this player was asked to upload this map's pack. */
    public static boolean isExpectedUpload(ServerPlayer player, String mapName) {
        return player.getUUID().equals(expectedUploader) && mapName.equals(expectedUploadMap);
    }

    public static void uploadFinished() {
        expectedUploader = null;
        expectedUploadMap = null;
    }

    /** Chunks saved with real data, read from the region files' location tables. */
    private static Set<ChunkPos> terrainChunks(Path regions) {
        Set<ChunkPos> chunks = new TreeSet<>((a, b) -> a.x != b.x ? Integer.compare(a.x, b.x) : Integer.compare(a.z, b.z));
        try (Stream<Path> files = Files.list(regions)) {
            for (Path file : files.toList()) {
                Matcher m = REGION_FILE.matcher(file.getFileName().toString());
                if (!m.matches()) continue;
                int regionX = Integer.parseInt(m.group(1));
                int regionZ = Integer.parseInt(m.group(2));
                if (Math.abs(regionX) > MAX_REGION_COORD || Math.abs(regionZ) > MAX_REGION_COORD) continue;
                if (Files.size(file) < 4096) continue;
                try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {
                    byte[] header = new byte[4096];
                    in.readFully(header);
                    for (int i = 0; i < 1024; i++) {
                        int sectorOffset = ((header[i * 4] & 0xFF) << 16) | ((header[i * 4 + 1] & 0xFF) << 8) | (header[i * 4 + 2] & 0xFF);
                        if (sectorOffset == 0 || sectorOffset * 4096L + 4 > in.length()) continue;
                        in.seek(sectorOffset * 4096L);
                        if (in.readInt() >= MIN_TERRAIN_BYTES) {
                            chunks.add(new ChunkPos(regionX * 32 + (i & 31), regionZ * 32 + (i >> 5)));
                        }
                    }
                }
            }
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not read map regions for the scan", e);
        }
        return chunks;
    }

    /** One stop per grid cell of {@code step} chunks that has terrain, in a back-and-forth order. */
    private static List<ChunkPos> planStops(Set<ChunkPos> chunks, int step) {
        TreeSet<Long> cells = new TreeSet<>();
        for (ChunkPos chunk : chunks) {
            cells.add(ChunkPos.asLong(Math.floorDiv(chunk.x, step), Math.floorDiv(chunk.z, step)));
        }
        List<ChunkPos> cellList = cells.stream().map(ChunkPos::new).sorted((a, b) -> {
            if (a.z != b.z) return Integer.compare(a.z, b.z);
            return (a.z % 2 == 0) ? Integer.compare(a.x, b.x) : Integer.compare(b.x, a.x);
        }).toList();
        List<ChunkPos> stops = new ArrayList<>();
        for (ChunkPos cell : cellList) {
            stops.add(new ChunkPos(cell.x * step + step / 2, cell.z * step + step / 2));
        }
        return stops;
    }
}
