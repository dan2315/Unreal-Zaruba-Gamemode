package com.dod.unrealzaruba.WorldManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Stream;

import com.dod.unrealzaruba.NetworkPackets.MapPack.ClientboundMapPackDataPacket;
import com.dod.unrealzaruba.NetworkPackets.MapPack.ClientboundMapPackOfferPacket;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Pre-rendered Xaero's World Map region files, so players see the whole map instead of only what they have explored.
 * Each /mapscan is saved under its name as unrealzaruba/xaero-maps/&lt;scan&gt;/ (the region .zip files) and becomes
 * the scan of the map it was made on (xaero-maps/maps.json, map key to scan; see {@link #scanFor}). When a match
 * starts, its map's scan is offered to everyone by hash; clients download it once in pieces and show it right away.
 * /mappack loads any scan for everyone, for checking it.
 */
public final class MapPacks {
    private static final Path ROOT = Path.of("unrealzaruba", "xaero-maps");
    private static final Path BINDINGS = ROOT.resolve("maps.json");
    private static final Path LEGACY_BINDINGS = ROOT.resolve("modes.json");
    public static final java.util.regex.Pattern NAME = java.util.regex.Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final int CHUNK_SIZE = 64 * 1024;
    private static final int CHUNKS_PER_TICK = 2;

    public record PackFile(String name, byte[] data) {}

    public record Pack(String mapName, String hash, List<PackFile> files) {
        public long totalSize() {
            return files.stream().mapToLong(file -> file.data().length).sum();
        }
    }

    private record Transfer(Pack pack, Queue<ClientboundMapPackDataPacket> chunks) {}

    private static final Map<String, Pack> PACKS = new HashMap<>();
    private static final Map<UUID, Transfer> TRANSFERS = new HashMap<>();
    private static Pack current;
    private static String currentScan;
    private static String currentMap;

    private MapPacks() {}

    /** A new match on this map: its scan (read again, so a newer upload is picked up) for everyone online. */
    public static void selectForMap(MinecraftServer server, String mapKey) {
        currentMap = mapKey;
        load(server, scanFor(mapKey));
    }

    /** Makes this scan the world map everyone online gets now. Null if there is no such scan (then nobody gets one). */
    public static Pack load(MinecraftServer server, String scanName) {
        PACKS.remove(scanName);
        currentScan = scanName;
        current = read(scanName);
        TRANSFERS.clear();
        if (server != null) {
            server.getPlayerList().getPlayers().forEach(MapPacks::offer);
        }
        return current;
    }

    /** Tells a player which world map to show: the current pack, or none (an offer without a hash). */
    public static void offer(ServerPlayer player) {
        if (current == null) {
            PacketDistributor.sendToPlayer(player, new ClientboundMapPackOfferPacket("", "", List.of()));
            return;
        }
        List<ClientboundMapPackOfferPacket.FileInfo> files = current.files().stream()
                .map(file -> new ClientboundMapPackOfferPacket.FileInfo(file.name(), file.data().length))
                .toList();
        PacketDistributor.sendToPlayer(player, new ClientboundMapPackOfferPacket(current.mapName(), current.hash(), files));
    }

    public static Pack current() {
        return current;
    }

    public static String currentScan() {
        return currentScan;
    }

    /** The map whose scan was selected last (a match start), or null. */
    public static String currentMap() {
        return currentMap;
    }

    /** The scans on disk: folders under xaero-maps with region files. */
    public static List<String> scans() {
        if (!Files.isDirectory(ROOT)) return List.of();
        try (Stream<Path> dirs = Files.list(ROOT)) {
            return dirs.filter(Files::isDirectory)
                    .map(dir -> dir.getFileName().toString())
                    .filter(name -> NAME.matcher(name).matches() && hasRegions(ROOT.resolve(name)))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not list the world map scans", e);
            return List.of();
        }
    }

    private static boolean hasRegions(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.anyMatch(file -> file.getFileName().toString().endsWith(".zip"));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * The scan a map shows: the one bound to it, else the scan named after it. A mode's old template falls back to the
     * scan named after the mode class (CapturePointsGamemode), where its pack was before maps had names.
     */
    public static String scanFor(String mapKey) {
        String bound = bindings().get(mapKey);
        if (bound != null) return bound;
        GameMaps.GameMap map = GameMaps.get(mapKey);
        var modeClass = map == null || !map.legacy() ? null : com.dod.unrealzaruba.Gamemodes.GamemodeFactory.classes.get(map.mode());
        return modeClass != null ? modeClass.getSimpleName() : mapKey;
    }

    public static void bind(String mapKey, String scanName) {
        Map<String, String> maps = new java.util.TreeMap<>(bindings());
        maps.put(mapKey, scanName);
        save(maps);
    }

    private static void save(Map<String, String> maps) {
        try {
            Files.createDirectories(ROOT);
            Files.writeString(BINDINGS, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(maps));
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not save {}", BINDINGS, e);
        }
    }

    /** Map key to scan. Bindings made per mode (modes.json, mode class to scan) become those of the modes' old templates. */
    private static Map<String, String> bindings() {
        if (!Files.exists(BINDINGS) && Files.exists(LEGACY_BINDINGS)) {
            Map<String, String> maps = new java.util.TreeMap<>();
            readMap(LEGACY_BINDINGS).forEach((modeClass, scan) -> maps.put(modeClass.toLowerCase(), scan));
            save(maps);
            try {
                Files.delete(LEGACY_BINDINGS);
            } catch (IOException e) {
                UnrealZaruba.LOGGER.warn("[UnrealZaruba] Could not remove {}", LEGACY_BINDINGS, e);
            }
            UnrealZaruba.LOGGER.info("[UnrealZaruba] World map scan bindings moved from {} to {}: {}", LEGACY_BINDINGS, BINDINGS, maps);
        }
        return readMap(BINDINGS);
    }

    private static Map<String, String> readMap(Path file) {
        if (!Files.exists(file)) return Map.of();
        try {
            Map<String, String> map = new com.google.gson.Gson().fromJson(Files.readString(file),
                    new com.google.gson.reflect.TypeToken<Map<String, String>>() {}.getType());
            return map == null ? Map.of() : map;
        } catch (IOException | com.google.gson.JsonParseException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not read {}", file, e);
            return Map.of();
        }
    }

    /** The client doesn't have the offered pack yet. */
    public static void request(ServerPlayer player, String hash) {
        if (current == null || !current.hash().equals(hash)) return;
        Queue<ClientboundMapPackDataPacket> chunks = new ArrayDeque<>();
        for (int fileIndex = 0; fileIndex < current.files().size(); fileIndex++) {
            byte[] data = current.files().get(fileIndex).data();
            for (int offset = 0; offset < data.length; offset += CHUNK_SIZE) {
                int length = Math.min(CHUNK_SIZE, data.length - offset);
                byte[] piece = new byte[length];
                System.arraycopy(data, offset, piece, 0, length);
                chunks.add(new ClientboundMapPackDataPacket(hash, fileIndex, offset, piece));
            }
        }
        TRANSFERS.put(player.getUUID(), new Transfer(current, chunks));
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Sending map pack {} ({} KB) to {}", current.mapName(),
                current.totalSize() / 1024, player.getName().getString());
    }

    /** Sends a few pieces per tick to each downloading player, so a pack doesn't flood the connection. */
    public static void tick(MinecraftServer server) {
        if (TRANSFERS.isEmpty()) return;
        for (var it = TRANSFERS.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Queue<ClientboundMapPackDataPacket> chunks = entry.getValue().chunks();
            if (player == null || entry.getValue().pack() != current) {
                it.remove();
                continue;
            }
            for (int i = 0; i < CHUNKS_PER_TICK && !chunks.isEmpty(); i++) {
                PacketDistributor.sendToPlayer(player, chunks.poll());
            }
            if (chunks.isEmpty()) {
                it.remove();
            }
        }
    }

    // ---- uploads after /mapscan ----

    private static final java.util.regex.Pattern REGION_FILE = java.util.regex.Pattern.compile("-?\\d+_-?\\d+\\.zip");
    private static final int MAX_UPLOAD_FILES = 4096;
    private static final int MAX_UPLOAD_FILE_SIZE = 32 * 1024 * 1024;

    private record Upload(String mapName, int fileCount, Map<String, Integer> sizes, Map<String, Long> received) {}

    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();

    public static void onUpload(ServerPlayer player, com.dod.unrealzaruba.NetworkPackets.MapPack.UploadMapPackPacket piece) {
        if (!player.hasPermissions(2) || !MapScan.isExpectedUpload(player, piece.mapName)) return;
        if (!NAME.matcher(piece.mapName).matches()) return;
        String mapKey = MapScan.uploadMap();
        if (piece.fileCount <= 0 || piece.fileCount > MAX_UPLOAD_FILES) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Nothing to upload: the client has no rendered map"));
            MapScan.uploadFinished();
            return;
        }
        if (!REGION_FILE.matcher(piece.fileName).matches() || piece.fileSize < 0 || piece.fileSize > MAX_UPLOAD_FILE_SIZE
                || piece.offset < 0 || piece.offset + piece.data.length > piece.fileSize) return;

        Upload upload = UPLOADS.computeIfAbsent(player.getUUID(),
                id -> new Upload(piece.mapName, piece.fileCount, new HashMap<>(), new HashMap<>()));
        if (!upload.mapName().equals(piece.mapName) || upload.fileCount() != piece.fileCount) {
            UPLOADS.remove(player.getUUID());
            return;
        }

        Path staging = ROOT.resolve(".upload-" + piece.mapName);
        try {
            Files.createDirectories(staging);
            try (java.io.RandomAccessFile out = new java.io.RandomAccessFile(staging.resolve(piece.fileName).toFile(), "rw")) {
                out.seek(piece.offset);
                out.write(piece.data);
            }
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not store uploaded map piece", e);
            return;
        }
        upload.sizes().put(piece.fileName, piece.fileSize);
        upload.received().merge(piece.fileName, (long) piece.data.length, Long::sum);

        boolean done = upload.sizes().size() == upload.fileCount() && upload.sizes().entrySet().stream()
                .allMatch(e -> upload.received().getOrDefault(e.getKey(), 0L) >= e.getValue());
        if (!done) return;

        UPLOADS.remove(player.getUUID());
        MapScan.uploadFinished();
        try {
            Path target = ROOT.resolve(piece.mapName);
            Files.createDirectories(target);
            try (Stream<Path> old = Files.list(target)) {
                for (Path file : old.filter(p -> p.getFileName().toString().endsWith(".zip")).toList()) {
                    Files.delete(file);
                }
            }
            for (String name : upload.sizes().keySet()) {
                Files.move(staging.resolve(name), target.resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            try (Stream<Path> rest = Files.list(staging)) {
                for (Path file : rest.toList()) Files.delete(file);
            }
            Files.delete(staging);
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not save uploaded map pack", e);
            return;
        }
        UnrealZaruba.LOGGER.info("[UnrealZaruba] {} uploaded the world map scan {} of {} ({} regions)",
                player.getName().getString(), piece.mapName, mapKey, upload.fileCount());
        if (mapKey != null) bind(mapKey, piece.mapName);
        load(player.server, piece.mapName);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Saved the world map scan " + piece.mapName
                + " (" + upload.fileCount() + " regions)" + (mapKey == null ? "" : ", now the world map of " + mapKey)));
    }

    private static Pack read(String mapName) {
        return PACKS.computeIfAbsent(mapName, name -> {
            Path dir = ROOT.resolve(name);
            if (!Files.isDirectory(dir)) {
                UnrealZaruba.LOGGER.info("[UnrealZaruba] No Xaero map pack for {} (looked in {})", name, dir.toAbsolutePath());
                return null;
            }
            try (Stream<Path> stream = Files.list(dir)) {
                List<Path> regionFiles = stream
                        .filter(path -> path.getFileName().toString().endsWith(".zip"))
                        .sorted()
                        .toList();
                if (regionFiles.isEmpty()) return null;

                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                List<PackFile> files = new ArrayList<>();
                for (Path path : regionFiles) {
                    byte[] data = Files.readAllBytes(path);
                    String fileName = path.getFileName().toString();
                    digest.update(fileName.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    digest.update(data);
                    files.add(new PackFile(fileName, data));
                }
                String hash = HexFormat.of().formatHex(digest.digest()).substring(0, 16);
                Pack pack = new Pack(name, hash, files);
                UnrealZaruba.LOGGER.info("[UnrealZaruba] Xaero map pack {}: {} regions, {} KB, hash {}", name,
                        files.size(), pack.totalSize() / 1024, hash);
                return pack;
            } catch (IOException | NoSuchAlgorithmException e) {
                UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not read Xaero map pack {}", name, e);
                return null;
            }
        });
    }
}
