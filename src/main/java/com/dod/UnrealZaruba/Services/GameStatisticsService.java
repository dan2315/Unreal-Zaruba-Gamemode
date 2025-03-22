package com.dod.UnrealZaruba.Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GameStatisticsService {

    public static final Gson jsonConverter = new GsonBuilder().create();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private HttpClientService httpClientService;

    public GameStatisticsService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    public void UpdatePlayerRanking(List<UUID> playersWhoWon, List<UUID> playersWhoLost) {
        executor.submit(() -> {
            try {
                HttpClient client = httpClientService.get();

                PlayerUpdates playerUpdates = new PlayerUpdates(playersWhoWon, playersWhoLost);
                String jsonInputString = jsonConverter.toJson(playerUpdates);
                UnrealZaruba.LOGGER.warn("JSON to send:" + jsonInputString);

                HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(HttpClientService.BASEURL + "/leaderboard/update"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInputString, StandardCharsets.UTF_8))
                .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Response Code: " + response.statusCode());
                System.out.println("Response Body: " + response.body());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
