package com.dod.unrealzaruba.Schematics;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dod.unrealfrontiers.ufessential.api.UFHttpClient;
import net.minecraft.client.Minecraft;

/**
 * Backend /schematics endpoints through Essential's {@link UFHttpClient}. Every call completes with a {@link Result};
 * network failures become an error result rather than an exceptional future.
 */
public final class SchematicsApi {
    public static final int PAGE_SIZE = 24;
    public static final int MAX_FILE_BYTES = 1024 * 1024;
    public static final int MAX_IMAGES = 5;
    public static final int MAX_NAME = 64;
    public static final int MAX_DESCRIPTION = 1000;

    private static final Gson GSON = new Gson();

    private SchematicsApi() {}

    public record Result<T>(T value, String error) {
        public boolean ok() {
            return error == null;
        }
    }

    /** An image to upload: file name, MIME type and bytes. */
    public record Upload(String filename, String contentType, byte[] data) {}

    public enum Sort {
        NEWEST("newest"), RATING("rating"), BATTLES("battles"), PRICE("price");

        public final String param;

        Sort(String param) {
            this.param = param;
        }
    }

    public static boolean isLoggedIn() {
        return UFHttpClient.isLoggedIn();
    }

    /** Runs the callback on the client thread once the call finishes. */
    public static <T> void then(CompletableFuture<Result<T>> future, Consumer<Result<T>> callback) {
        future.thenAccept(result -> Minecraft.getInstance().execute(() -> callback.accept(result)));
    }

    public static CompletableFuture<Result<SchematicDto.Page>> list(boolean mine, String query, Sort sort, int page) {
        StringBuilder path = new StringBuilder(mine ? "/schematics/my" : "/schematics")
                .append("?sort=").append(sort.param)
                .append("&page=").append(page)
                .append("&pageSize=").append(PAGE_SIZE);
        if (query != null && !query.isBlank()) {
            String q = query.strip();
            path.append("&q=").append(encode(q.length() > 64 ? q.substring(0, 64) : q));
        }
        return json(UFHttpClient.get(path.toString()), SchematicDto.Page.class);
    }

    /** How many schematics the player has uploaded and how many they may have (limit is null when there is no limit). */
    public record Limit(int count, Integer limit, Integer remaining) {}

    public static CompletableFuture<Result<Limit>> limit() {
        return json(UFHttpClient.get("/schematics/limit"), Limit.class);
    }

    public static CompletableFuture<Result<SchematicDto>> get(long id) {
        return json(UFHttpClient.get("/schematics/" + id), SchematicDto.class);
    }

    /** Price breakdown; fetched only when a schematic is opened. */
    public static CompletableFuture<Result<SchematicPriceDto>> price(long id) {
        return json(UFHttpClient.get("/schematics/" + id + "/price"), SchematicPriceDto.class);
    }

    public static CompletableFuture<Result<SchematicDto>> upload(String name, String description, boolean isPublic,
                                                                 String filename, byte[] file, List<Upload> images) {
        UFHttpClient.Multipart form = new UFHttpClient.Multipart()
                .file("file", filename, "application/octet-stream", file)
                .field("name", name)
                .field("isPublic", Boolean.toString(isPublic));
        if (description != null && !description.isBlank()) {
            form.field("description", description);
        }
        for (Upload image : images) {
            form.file("images", image.filename(), image.contentType(), image.data());
        }
        return json(UFHttpClient.sendMultipart("POST", "/schematics", form), SchematicDto.class);
    }

    /** Changes only the non-null fields. An empty description removes it. */
    public static CompletableFuture<Result<SchematicDto>> update(long id, String name, String description, Boolean isPublic) {
        JsonObject body = new JsonObject();
        if (name != null) body.addProperty("name", name);
        if (description != null) body.addProperty("description", description);
        if (isPublic != null) body.addProperty("isPublic", isPublic);
        return json(UFHttpClient.send("PATCH", "/schematics/" + id, body.toString()), SchematicDto.class);
    }

    public static CompletableFuture<Result<Void>> delete(long id) {
        return empty(UFHttpClient.delete("/schematics/" + id));
    }

    public static CompletableFuture<Result<SchematicDto>> replaceFile(long id, String filename, byte[] file) {
        UFHttpClient.Multipart form = new UFHttpClient.Multipart()
                .file("file", filename, "application/octet-stream", file);
        return json(UFHttpClient.sendMultipart("PUT", "/schematics/" + id + "/file", form), SchematicDto.class);
    }

    public static CompletableFuture<Result<Void>> rate(long id, int score) {
        JsonObject body = new JsonObject();
        body.addProperty("score", score);
        return empty(UFHttpClient.putJson("/schematics/" + id + "/rating", body.toString()));
    }

    public static CompletableFuture<Result<byte[]>> download(long id) {
        return bytes("/schematics/" + id + "/download");
    }

    /** @param url image path as the backend gives it, e.g. "/schematics/1/images/5" */
    public static CompletableFuture<Result<byte[]>> image(String url) {
        return bytes(url);
    }

    public static CompletableFuture<Result<List<SchematicDto.Image>>> addImages(long id, List<Upload> images) {
        UFHttpClient.Multipart form = new UFHttpClient.Multipart();
        for (Upload image : images) {
            form.file("images", image.filename(), image.contentType(), image.data());
        }
        return json(UFHttpClient.sendMultipart("POST", "/schematics/" + id + "/images", form), SchematicDto.Image[].class)
                .thenApply(SchematicsApi::asList);
    }

    public static CompletableFuture<Result<Void>> deleteImage(long id, long imageId) {
        return empty(UFHttpClient.delete("/schematics/" + id + "/images/" + imageId));
    }

    /** Every image once; the first becomes the cover. */
    public static CompletableFuture<Result<Void>> reorderImages(long id, List<Long> imageIds) {
        JsonArray ids = new JsonArray();
        imageIds.forEach(ids::add);
        JsonObject body = new JsonObject();
        body.add("imageIds", ids);
        return empty(UFHttpClient.putJson("/schematics/" + id + "/images/order", body.toString()));
    }

    // ---------- plumbing ----------

    private static <T> CompletableFuture<Result<T>> json(CompletableFuture<UFHttpClient.Response> call, Class<T> type) {
        return handle(call, r -> {
            if (!r.isSuccess()) return new Result<>(null, r.errorMessage());
            try {
                return new Result<>(GSON.fromJson(r.body(), type), null);
            } catch (RuntimeException e) {
                return new Result<>(null, "Bad response: " + e.getMessage());
            }
        });
    }

    private static CompletableFuture<Result<Void>> empty(CompletableFuture<UFHttpClient.Response> call) {
        return handle(call, r -> r.isSuccess() ? new Result<>(null, null) : new Result<>(null, r.errorMessage()));
    }

    private static CompletableFuture<Result<byte[]>> bytes(String path) {
        return handle(UFHttpClient.getBytes(path),
                r -> r.isSuccess() ? new Result<>(r.body(), null) : new Result<>(null, r.errorMessage()));
    }

    private static <R, T> CompletableFuture<Result<T>> handle(CompletableFuture<R> call, Function<R, Result<T>> map) {
        return call.handle((r, error) -> {
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                return new Result<>(null, "Backend unreachable: " + cause.getClass().getSimpleName()
                        + (cause.getMessage() != null ? " " + cause.getMessage() : ""));
            }
            return map.apply(r);
        });
    }

    private static <T> Result<List<T>> asList(Result<T[]> result) {
        return result.ok() ? new Result<>(Arrays.asList(result.value()), null) : new Result<>(null, result.error());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
