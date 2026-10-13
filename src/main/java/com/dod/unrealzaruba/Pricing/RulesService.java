package com.dod.unrealzaruba.Pricing;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.dod.unrealzaruba.Services.HttpClientService;
import com.dod.unrealzaruba.UnrealZaruba;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.neoforged.fml.loading.FMLEnvironment;

/**
 * The backend schematic rules: the price list, the forbidden things and the size limit (GET /schematics/rules). Kept in
 * memory and refreshed in the background once a minute while somebody uses them; the last good copy is kept when the
 * backend cannot be reached.
 */
public final class RulesService {
    private static final long REFRESH_MILLIS = 60_000;
    private static final String API_KEY_VARIABLE = "UF_API_KEY";

    /** version changes whenever anything in the rules changes; scanner is the backend scanner version. */
    public record Rules(String version, int scanner, int[] maxSize, PriceCatalog catalog, long fetchedAtMillis) {}

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final AtomicBoolean REFRESHING = new AtomicBoolean();

    private static volatile Rules rules;
    private static volatile String etag;
    private static volatile long lastAttemptMillis;
    private static volatile boolean scannerWarned;

    private RulesService() {}

    /** The current rules, or null if they have never been fetched. Starts a refresh when they are stale. */
    public static Rules get() {
        if (System.currentTimeMillis() - lastAttemptMillis > REFRESH_MILLIS) refreshAsync();
        return rules;
    }

    public static void refreshAsync() {
        if (!REFRESHING.compareAndSet(false, true)) return;
        lastAttemptMillis = System.currentTimeMillis();
        Thread thread = new Thread(() -> {
            try {
                fetch();
            } catch (Exception e) {
                UnrealZaruba.LOGGER.warn("[Rules] Could not fetch the schematic rules: {}", e.toString());
            } finally {
                REFRESHING.set(false);
            }
        }, "schematic-rules");
        thread.setDaemon(true);
        thread.start();
    }

    private static void fetch() throws Exception {
        String key = System.getenv(API_KEY_VARIABLE);
        if (key == null || key.isBlank()) {
            fetchAsPlayer();
            return;
        }
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(HttpClientService.baseUrl() + "/schematics/rules"))
                .timeout(Duration.ofSeconds(15))
                .GET();
        request.header("x-api-key", key);
        if (etag != null && rules != null) request.header("If-None-Match", etag);

        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 304 && rules != null) {
            rules = new Rules(rules.version(), rules.scanner(), rules.maxSize(), rules.catalog(), System.currentTimeMillis());
            return;
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("answered " + response.statusCode());
        }
        rules = parse(response.body());
        etag = response.headers().firstValue("ETag").orElse(null);
        UnrealZaruba.LOGGER.info("[Rules] Schematic rules {}: {} price entries, max size {}x{}x{}", rules.version(),
                rules.catalog().size(), rules.maxSize()[0], rules.maxSize()[1], rules.maxSize()[2]);
        if (rules.scanner() != CompositionScanner.VERSION && !scannerWarned) {
            scannerWarned = true;
            UnrealZaruba.LOGGER.warn("[Rules] The backend scanner is version {} but this mod implements {}: prices shown in game may differ",
                    rules.scanner(), CompositionScanner.VERSION);
        }
    }

    /** No server key: on a client the logged-in player may read the rules themselves (single player, a world of their own). */
    private static void fetchAsPlayer() throws Exception {
        if (!FMLEnvironment.dist.isClient()) {
            throw new IllegalStateException("no " + API_KEY_VARIABLE + " on a dedicated server");
        }
        String body = ClientRulesFetch.fetch();
        if (body == null) throw new IllegalStateException("not logged in, or the backend refused the rules");
        rules = parse(body);
        etag = null;
        UnrealZaruba.LOGGER.info("[Rules] Schematic rules {} read as the player: {} price entries", rules.version(), rules.catalog().size());
    }

    /** Parses the GET /schematics/rules document. */
    public static Rules parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray size = root.getAsJsonArray("maxSize");
        int[] maxSize = {size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt()};
        List<PriceCatalog.Entry> entries = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("prices")) {
            JsonObject entry = element.getAsJsonObject();
            boolean forbidden = entry.has("forbidden") && entry.get("forbidden").getAsBoolean();
            entries.add(new PriceCatalog.Entry(entry.get("kind").getAsString(), entry.get("id").getAsString(),
                    new BigDecimal(entry.get("price").getAsString()), forbidden));
        }
        String version = root.has("rulesVersion") ? root.get("rulesVersion").getAsString() : "?";
        int scanner = root.has("scanner") ? root.get("scanner").getAsInt() : 0;
        return new Rules(version, scanner, maxSize, new PriceCatalog(entries), System.currentTimeMillis());
    }
}
