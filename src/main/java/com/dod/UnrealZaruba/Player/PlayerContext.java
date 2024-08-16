package com.dod.UnrealZaruba.Player;

import java.util.UUID;

import com.dod.UnrealZaruba.Events.PlayerStatus;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import java.util.HashMap;

import net.minecraft.world.level.GameType;

public class PlayerContext {

    public static HashMap<UUID, PlayerContext> playerUlist = new HashMap<>();
    private UUID id;
    private boolean authorized;
    private GameType originalGameType;
    private PlayerStatus status;

    private BaseGamemode gamemode;

    public BaseGamemode Gamemode() { return gamemode; } 
    public <T extends BaseGamemode> T Gamemode(Class<T> clazz) { 
        if (clazz.isInstance(gamemode)) {
            return clazz.cast(gamemode);
        } else {
            throw new ClassCastException("Gamemode cannot be cast to the specified type.");
        }
    }
    
    public static PlayerContext Instantiate(UUID id, GameType gameType) {
        PlayerContext playerU = new PlayerContext();
        playerU.id = id;
        playerU.originalGameType = gameType;

        playerUlist.put(id, playerU);
        return playerU;
    }

    public static PlayerContext Get(UUID id) {
        return playerUlist.get(id);
    }

    public void SetGamemode(BaseGamemode gamemode) {
        this.gamemode = gamemode;
    }

    public void SetAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public boolean IsAuthorized() {
        return this.authorized;
    }
    
    public static void Deauthorize(UUID id) {
        playerUlist.remove(id);
    }
}

