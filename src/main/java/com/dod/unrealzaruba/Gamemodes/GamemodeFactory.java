package com.dod.unrealzaruba.Gamemodes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import com.dod.unrealzaruba.Services.GameStatisticsService;
import com.dod.unrealzaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.unrealzaruba.Vehicles.VehicleManager;

public class GamemodeFactory {
    public static final HashMap<String, Supplier<BaseGamemode>> gamemodes = new HashMap<>();
    /** Mode id (as in the lobby and in map.json) to its class; the class name is the mode's gamemodedata folder. */
    public static final Map<String, Class<? extends BaseGamemode>> classes = Map.of(
            DestroyObjectivesGamemode.GAMEMODE_NAME, DestroyObjectivesGamemode.class,
            CapturePointsGamemode.GAMEMODE_NAME, CapturePointsGamemode.class,
            ShipsGamemode.GAMEMODE_NAME, ShipsGamemode.class);

    public static void Initialize(VehicleManager vehicleManager, GameStatisticsService gameStatisticsService, IGameTimer gameTimer) {
        gamemodes.put(DestroyObjectivesGamemode.GAMEMODE_NAME, () -> new DestroyObjectivesGamemode(gameStatisticsService, gameTimer));
        gamemodes.put(CapturePointsGamemode.GAMEMODE_NAME, () -> new CapturePointsGamemode(gameStatisticsService, gameTimer));
        gamemodes.put(ShipsGamemode.GAMEMODE_NAME, () -> new ShipsGamemode(vehicleManager, gameStatisticsService, gameTimer));
    }

    public static BaseGamemode createGamemode(String gamemodeName) {
        return gamemodes.get(gamemodeName).get();
    }

    /** The mode id of a gamemode class, or null for a class the factory doesn't make. */
    public static String idOf(Class<? extends BaseGamemode> gamemodeClass) {
        for (var entry : classes.entrySet()) {
            if (entry.getValue() == gamemodeClass) return entry.getKey();
        }
        return null;
    }
}
