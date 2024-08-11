package com.dod.UnrealZaruba.Gamemodes;

import net.minecraft.world.level.Level;
import java.util.HashMap;

public class GamemodeManager {
    public static HashMap<Level, BaseGamemode> worldToGamemode = new HashMap<>();


    public static void StartGame(Level level, BaseGamemode gamemode) {
        worldToGamemode.put(level, gamemode);
    }

    public static BaseGamemode Get(Level level) {
        return worldToGamemode.get(level);
    }
}
