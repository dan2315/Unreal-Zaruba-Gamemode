package com.dod.UnrealZaruba.DiscordIntegration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.dod.UnrealZaruba.UnrealZaruba;

import java.util.Set;
import java.util.HashSet;

public class DiscordAuth {
    public static Set<String> unresolvedRequests = new HashSet<String>();
    public static String backendEndpoint = "http://34.34.44.237:3000";

    public static void OpenAuthPage(String state, UUID playerUuid, String minecraft_username, Integer port) {
        try {
            String authUrl = backendEndpoint
                    + "/auth?state=" + state
                    + "&player_uuid=" + playerUuid
                    + "&minecraft_username=" + minecraft_username
                    + "&port=" + port;
            openURL(authUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void openURL(String url) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", url });
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[] { "open", url });
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[] { "xdg-open", url });
            } else {
                throw new UnsupportedOperationException("Unsupported operating system");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void CheckAuthTokens(UUID playerUUID, Integer port, String token, String refreshTokens) {
        HttpClient client = HttpClient.newHttpClient();

        String queryParams;
        try {
            queryParams = String.format("player_uuid=%s&port=%s&token=%s&refreshTokens=%s",
                    URLEncoder.encode(playerUUID.toString(), StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(port.toString(), StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(token.toString(), StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(refreshTokens.toString(), StandardCharsets.UTF_8.toString()));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(DiscordAuth.backendEndpoint + "/check_tokens?" + queryParams))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

            
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
