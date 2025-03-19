package com.dod.UnrealZaruba.Gamemodes;

import net.minecraft.world.level.Level;
import java.util.HashMap;

public class GamemodeManager {
    public static HashMap<Level, BaseGamemode> worldToGamemode = new HashMap<>();


    public static void InitializeGamemode(Level level, BaseGamemode gamemode) {
        gamemode.Initialize();
        worldToGamemode.put(level, gamemode);
    }

    public static BaseGamemode Get(Level level) {
        return worldToGamemode.get(level);
    }

    public static <T extends BaseGamemode> T Get(Level level, Class<T> gamemodeClass) {
        BaseGamemode gamemode = worldToGamemode.get(level);
        if (gamemodeClass.isInstance(gamemode)) {
            return gamemodeClass.cast(gamemode);
        } else {
            return null;
        }
    }
}
