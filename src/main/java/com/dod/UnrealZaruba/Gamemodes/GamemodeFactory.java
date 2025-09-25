package com.dod.unrealzaruba.Gamemodes;

import java.util.HashMap;
import java.util.function.Supplier;
import com.dod.unrealzaruba.Services.GameStatisticsService;
import com.dod.unrealzaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.unrealzaruba.Vehicles.VehicleManager;

public class GamemodeFactory {
    public static final HashMap<String, Supplier<BaseGamemode>> gamemodes = new HashMap<>();

    public static void Initialize(VehicleManager vehicleManager, GameStatisticsService gameStatisticsService, IGameTimer gameTimer) {
        gamemodes.put(DestroyObjectivesGamemode.GAMEMODE_NAME, () -> new DestroyObjectivesGamemode(gameStatisticsService, gameTimer));
        gamemodes.put(CapturePointsGamemode.GAMEMODE_NAME, () -> new CapturePointsGamemode(gameStatisticsService, gameTimer));
        gamemodes.put(ShipsGamemode.GAMEMODE_NAME, () -> new ShipsGamemode(vehicleManager, gameStatisticsService, gameTimer));
    }

    public static BaseGamemode createGamemode(String gamemodeName) {
        return gamemodes.get(gamemodeName).get();
    }
}
