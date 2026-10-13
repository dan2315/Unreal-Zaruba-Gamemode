package com.dod.unrealzaruba.WorldManager;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.Services.HttpClientService;
import com.dod.unrealzaruba.UnrealZaruba;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Commits the current map (or the lobby) to the backend as a new world version: POST /worlds/{serverId}/commit with the
 * server's API key. The archive is zipped on a worker thread straight into the request body, so the map is never held
 * in memory. Only one commit runs at a time.
 *
 * <p>Gamemode maps are the template folders under game_worlds/ (zipped as game_worlds/&lt;slot&gt;/...); the lobby is the
 * server's own level folder. In DEV mode the game level IS its template, and the lobby is always live, so those levels
 * are saved first and autosave is held off while the archive is written.
 */
public final class WorldCommit {
    public static final String LOBBY = "lobby";
    /** Same keys as the backend's slots: lobby plus the lowercase gamemode class names. */
    public static final List<String> SLOTS = List.of(LOBBY, "capturepointsgamemode", "destroyobjectivesgamemode",
            "shipsgamemode", "mesilovogamemode");

    private static final String API_KEY_VARIABLE = "UF_API_KEY";
    private static final String SERVER_ID_VARIABLE = "UF_SERVER_ID";
    private static final String DEFAULT_SERVER_ID = "zaruba";
    private static final int MAX_COMMENT = 200;
    private static final String DIMENSION_PATH = "dimensions/unrealzaruba/game_dim";
    /** Per-player data in the lobby level: not part of the map, and the server keeps its own on apply. */
    private static final List<String> LOBBY_SKIPPED_FOLDERS = List.of("playerdata", "stats", "advancements");

    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    // HTTP/1.1 only: a streamed body of unknown length must go out chunked, not through an h2c upgrade attempt
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private WorldCommit() {}

    /** Starts a commit; call on the server thread. Reports progress and the result to the command source. */
    public static int start(CommandSourceStack source, String slot, String comment) {
        MinecraftServer server = source.getServer();
        if (!SLOTS.contains(slot)) {
            source.sendFailure(Component.literal("Unknown slot '" + slot + "'. Slots: " + String.join(", ", SLOTS)));
            return 0;
        }
        String apiKey = System.getenv(API_KEY_VARIABLE);
        if (apiKey == null || apiKey.isBlank()) {
            source.sendFailure(Component.literal("This server has no " + API_KEY_VARIABLE
                    + " environment variable, so it can't talk to the backend as a server."));
            return 0;
        }
        Path root = slot.equals(LOBBY)
                ? server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize()
                : Path.of("game_worlds", slot).toAbsolutePath().normalize();
        boolean live = slot.equals(LOBBY) || ConfigManager.isDevMode() && slot.equals(WorldManager.currentMapKey());
        Path required = slot.equals(LOBBY) ? root.resolve("level.dat") : root.resolve(DIMENSION_PATH).resolve("region");
        if (!Files.exists(required)) {
            source.sendFailure(Component.literal("Nothing to commit: " + root + " has no "
                    + (slot.equals(LOBBY) ? "level.dat" : "saved map (" + DIMENSION_PATH + "/region)") + "."));
            return 0;
        }
        if (WorldManager.isCopyingTemplate(slot)) {
            source.sendFailure(Component.literal("The map template '" + slot
                    + "' is being copied for a match right now. Try again in a few seconds."));
            return 0;
        }
        if (!RUNNING.compareAndSet(false, true)) {
            source.sendFailure(Component.literal("Another world commit is already running."));
            return 0;
        }

        Map<ServerLevel, Boolean> autosave = new IdentityHashMap<>();
        try {
            if (live) {
                // Write what the game holds in memory, then keep autosave from touching the files while they are read
                server.saveAllChunks(false, true, true);
                for (ServerLevel level : server.getAllLevels()) {
                    autosave.put(level, level.noSave);
                    level.noSave = true;
                }
            }
        } catch (RuntimeException e) {
            restoreAutosave(autosave);
            RUNNING.set(false);
            source.sendFailure(Component.literal("Saving the world failed: " + e.getMessage()));
            return 0;
        }

        String uploader = uploaderName(source);
        String cleanComment = cleanComment(comment);
        String serverId = envOr(SERVER_ID_VARIABLE, DEFAULT_SERVER_ID);
        source.sendSuccess(() -> Component.literal("Committing '" + slot + "' to the backend, zipping and uploading..."), true);
        UnrealZaruba.LOGGER.info("[WorldCommit] {} commits slot {} ({})", uploader, slot, root);

        CompletableFuture.supplyAsync(() -> upload(root, slot, slot.equals(LOBBY), serverId, apiKey, uploader, cleanComment))
                .whenComplete((outcome, error) -> server.execute(() -> {
                    restoreAutosave(autosave);
                    RUNNING.set(false);
                    if (error != null) {
                        UnrealZaruba.LOGGER.error("[WorldCommit] failed", error);
                        source.sendFailure(Component.literal("Commit failed: " + error));
                    } else if (outcome.ok()) {
                        UnrealZaruba.LOGGER.info("[WorldCommit] {}", outcome.message());
                        source.sendSuccess(() -> Component.literal(outcome.message()), true);
                    } else {
                        UnrealZaruba.LOGGER.warn("[WorldCommit] {}", outcome.message());
                        source.sendFailure(Component.literal(outcome.message()));
                    }
                }));
        return 1;
    }

    private static void restoreAutosave(Map<ServerLevel, Boolean> previous) {
        previous.forEach((level, noSave) -> level.noSave = noSave);
    }

    private record Outcome(boolean ok, String message) {}

    private static Outcome upload(Path root, String slot, boolean lobby, String serverId, String apiKey, String uploader, String comment) {
        AtomicReference<IOException> zipError = new AtomicReference<>();
        AtomicLong files = new AtomicLong();
        AtomicLong bytes = new AtomicLong();
        PipedInputStream zipped;
        PipedOutputStream sink;
        try {
            zipped = new PipedInputStream(1 << 20);
            sink = new PipedOutputStream(zipped);
        } catch (IOException e) {
            return new Outcome(false, "Commit failed: " + e.getMessage());
        }
        Thread writer = new Thread(() -> {
            try (ZipOutputStream zip = new ZipOutputStream(sink)) {
                zip.setLevel(Deflater.DEFAULT_COMPRESSION);
                writeZip(zip, root, lobby ? "" : "game_worlds/" + slot + "/", lobby, files, bytes);
            } catch (IOException e) {
                zipError.set(e);
            }
        }, "world-commit-zip");
        writer.setDaemon(true);
        writer.start();

        // A zip that failed halfway must not look like a finished one: the read side throws instead of reporting EOF
        InputStream zipBody = new FilterInputStream(zipped) {
            @Override
            public int read() throws IOException {
                int b = super.read();
                if (b < 0) check();
                return b;
            }

            @Override
            public int read(byte[] buffer, int off, int len) throws IOException {
                int n = super.read(buffer, off, len);
                if (n < 0) check();
                return n;
            }

            private void check() throws IOException {
                try {
                    writer.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted");
                }
                IOException failure = zipError.get();
                if (failure != null) throw new IOException("zipping failed: " + failure.getMessage(), failure);
            }
        };

        String boundary = "----ufworld" + UUID.randomUUID().toString().replace("-", "");
        StringBuilder head = new StringBuilder();
        field(head, boundary, "uploadedBy", uploader);
        field(head, boundary, "slot", slot);
        if (!comment.isEmpty()) field(head, boundary, "comment", comment);
        head.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(slot).append(".zip\"\r\n")
                .append("Content-Type: application/zip\r\n\r\n");
        byte[] prefix = head.toString().getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HttpClientService.baseUrl() + "/worlds/" + serverId + "/commit"))
                .header("x-api-key", apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofMinutes(30))
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new SequenceInputStream(Collections.enumeration(List.of(
                        new java.io.ByteArrayInputStream(prefix), zipBody, new java.io.ByteArrayInputStream(suffix))))))
                .build();
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return interpret(response.statusCode(), response.body(), slot, files.get(), bytes.get());
        } catch (IOException e) {
            IOException failure = zipError.get();
            return new Outcome(false, "Commit failed: " + (failure != null ? "zipping failed: " + failure.getMessage() : e.toString()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Outcome(false, "Commit interrupted");
        } finally {
            try {
                zipped.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static Outcome interpret(int status, String body, String slot, long files, long bytes) {
        JsonObject json = parse(body);
        if (status == 201) {
            String version = json != null && json.has("version") ? json.get("version").getAsString() : "?";
            return new Outcome(true, "Version " + version + " of '" + slot + "' uploaded (" + files + " files, "
                    + megabytes(bytes) + " MB unpacked). Activate it in the admin panel.");
        }
        StringBuilder message = new StringBuilder();
        if (status == 401) {
            message.append("The backend rejected the API key (401).");
        } else if (status == 400) {
            message.append("The backend refused the archive (400)");
            if (json != null && json.has("error")) message.append(": ").append(json.get("error").getAsString());
            if (json != null && json.has("problems") && json.get("problems").isJsonArray()) {
                JsonArray problems = json.getAsJsonArray("problems");
                for (JsonElement problem : problems) message.append("\n - ").append(problem.getAsString());
            }
        } else {
            message.append("The backend answered ").append(status);
            if (json != null && json.has("error")) message.append(": ").append(json.get("error").getAsString());
        }
        return new Outcome(false, message.toString());
    }

    private static JsonObject parse(String body) {
        try {
            JsonElement element = JsonParser.parseString(body);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void writeZip(ZipOutputStream zip, Path root, String prefix, boolean lobby, AtomicLong files, AtomicLong bytes) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (!Files.isRegularFile(path)) continue;
                Path relative = root.relativize(path);
                if (relative.getFileName().toString().equals("session.lock")) continue;
                if (lobby && LOBBY_SKIPPED_FOLDERS.contains(relative.getName(0).toString()) && relative.getNameCount() > 1) continue;
                // Entry names always use '/', whatever the platform's separator is
                StringBuilder name = new StringBuilder(prefix);
                for (int i = 0; i < relative.getNameCount(); i++) {
                    if (i > 0) name.append('/');
                    name.append(relative.getName(i));
                }
                ZipEntry entry = new ZipEntry(name.toString());
                entry.setTime(Files.getLastModifiedTime(path).toMillis());
                zip.putNextEntry(entry);
                bytes.addAndGet(Files.copy(path, zip));
                zip.closeEntry();
                files.incrementAndGet();
            }
        }
    }

    private static void field(StringBuilder out, String boundary, String name, String value) {
        out.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
                .append(value).append("\r\n");
    }

    /** The backend takes 1-32 characters of A-Za-z0-9_. */
    private static String uploaderName(CommandSourceStack source) {
        String name = source.getTextName().replaceAll("[^A-Za-z0-9_]", "_");
        if (name.length() > 32) name = name.substring(0, 32);
        return name.isEmpty() ? "console" : name;
    }

    private static String cleanComment(String comment) {
        if (comment == null) return "";
        String clean = comment.replaceAll("\\p{Cntrl}", " ").strip();
        return clean.length() > MAX_COMMENT ? clean.substring(0, MAX_COMMENT) : clean;
    }

    private static String envOr(String variable, String fallback) {
        String value = System.getenv(variable);
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static String megabytes(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f", bytes / (1024.0 * 1024.0));
    }
}
