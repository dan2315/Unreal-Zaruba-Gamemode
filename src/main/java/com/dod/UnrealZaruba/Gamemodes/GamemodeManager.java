package com.dod.UnrealZaruba.Gamemodes;

import java.util.HashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

public class GamemodeManager {
    public static HashMap<ResourceKey<Level>, BaseGamemode> worldToGamemode = new HashMap<>();


    public static void InitializeGamemode(Pair<ResourceKey<Level>, ResourceKey<Level>> dimensions, BaseGamemode gamemode) {
        gamemode.Initialize();
        worldToGamemode.put(dimensions.getLeft(), gamemode); // It was suggested by Deepseek, it kinda works
        worldToGamemode.put(dimensions.getRight(), gamemode);
    }

    public static BaseGamemode Get(ResourceKey<Level> level) {
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
