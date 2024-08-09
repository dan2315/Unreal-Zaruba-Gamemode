package com.dod.UnrealZaruba.DiscordIntegration;

import com.sun.net.httpserver.HttpServer;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import com.sun.net.httpserver.HttpHandler;
import com.dod.UnrealZaruba.unrealzaruba;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

public class CallbackServer {
    private static HttpServer server;
    private static final Set<UUID> authorizedPlayers = new HashSet<UUID>();

    public static void StartServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(8001), 0);

            // Define a context for handling /notifyAuthStatus POST requests
            server.createContext("/notifyAuthStatus", new NotifyAuthStatusHandler());

            server.setExecutor(null); // Use default executor
            server.start();

            unrealzaruba.LOGGER.info("HTTP server started on port 8001");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void StopServer() {
        if (server != null) {
            server.stop(3); // Delay of 1 second before stopping
            unrealzaruba.LOGGER.info("HTTP server stopped");
        }
    }

    public static Boolean isPlayerAuthorized(UUID uuid) {
        return authorizedPlayers.contains(uuid);
    }

    public static void DeauthorizeUser(UUID uuid) {
        authorizedPlayers.remove(uuid);
    }

    // Handler for processing /notifyAuthStatus POST requests
    static class NotifyAuthStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                var playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();
                InputStream inputStream = exchange.getRequestBody();
                String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                inputStream.close();

                Map<String, String> params = parseJsonBody(requestBody);

                String uuid = params.get("uuid");
                String state = params.get("state");
                if (!DiscordAuth.unresolvedRequests.contains(state)) {
                    String response = String.format("Dolbayob: [%s] | POPYTKA VZLOMA, ЫАЫАЫАЫАЫАЫА", playerList.getPlayer(UUID.fromString(uuid)));
                    unrealzaruba.LOGGER.info(response);
                    return;
                }
                else {
                    DiscordAuth.unresolvedRequests.remove(state);
                    unrealzaruba.LOGGER.info("Unresolved requests remaining: " + DiscordAuth.unresolvedRequests.size());
                }

                boolean isAuthenticated = Boolean.parseBoolean(params.get("authenticated"));

                if (uuid != null) {
                    UUID playerUUID = UUID.fromString(uuid);
                    SetPlayerAuthorized(uuid, isAuthenticated);
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                    TitleMessage.sendTitle(player, "Добро пожаловать, §2§r!");
                    server.sendMessage(new TextComponent("Авторизация прошла успешно"), playerUUID);

                    // Send a response
                    String response = String.format("Auth status for player [%s] set to [%b]", playerList.getPlayer(UUID.fromString(uuid)), isAuthenticated);
                    exchange.sendResponseHeaders(200, response.length());
                    unrealzaruba.LOGGER.info(response);
                } else {
                    // Send a bad request response
                    String response = "Invalid request: UUID is missing.";
                    exchange.sendResponseHeaders(400, response.length());

                }
            } else {
                // Send a method not allowed response
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }

        // Example method to notify a player
        private void SetPlayerAuthorized(String uuid, boolean isAuthenticated) {
            unrealzaruba.LOGGER.info("User with " + uuid + " " + (isAuthenticated ? "Krasava" : "Pidor"));
            authorizedPlayers.add(UUID.fromString(uuid));
        }

        // Parse JSON formatted body to map
        private Map<String, String> parseJsonBody(String jsonBody) {
            Map<String, String> map = new HashMap<>();
            // Assume simple JSON parsing, you might want to use a library like Jackson or
            // Gson
            jsonBody = jsonBody.replaceAll("[{}\"]", ""); // Simple cleanup
            for (String pair : jsonBody.split(",")) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    map.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
            return map;
        }

        
    }
}