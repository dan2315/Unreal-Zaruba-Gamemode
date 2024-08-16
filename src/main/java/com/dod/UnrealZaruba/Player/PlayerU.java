package com.dod.UnrealZaruba.Player;

import java.util.UUID;

import com.dod.UnrealZaruba.Events.PlayerStatus;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import java.util.HashMap;

import net.minecraft.world.level.GameType;

public class PlayerU  {

    public static HashMap<UUID, PlayerU> playerUlist = new HashMap<>();
    private UUID id;
    private boolean authorized;
    private GameType originalGameType;
    private PlayerStatus status;
    private boolean previouslyOpped;

    private BaseGamemode gamemode;


    public boolean PreviouslyOpped() {
        return previouslyOpped;
    }

    public BaseGamemode Gamemode() { return gamemode; } 
    public <T extends BaseGamemode> T Gamemode(Class<T> clazz) { 
        if (clazz.isInstance(gamemode)) {
            return clazz.cast(gamemode);
        } else {
            throw new ClassCastException("Gamemode cannot be cast to the specified type.");
        }
    }
    
    public static PlayerU Instantiate(UUID id, GameType gameType) {
        PlayerU playerU = new PlayerU();
        playerU.id = id;
        playerU.originalGameType = gameType;

        playerUlist.put(id, playerU);
        return playerU;
    }

    public static PlayerU Get(UUID id) {
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

    public void SetPreviouslyOpped() {
        previouslyOpped = true;
    }
}

