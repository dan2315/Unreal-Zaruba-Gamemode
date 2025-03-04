package com.dod.UnrealZaruba.Player;

import java.util.UUID;

import com.dod.UnrealZaruba.Events.PlayerStatus;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import java.util.HashMap;

import net.minecraft.world.level.GameType;

public class PlayerContext {

    public static HashMap<UUID, PlayerContext> playerContextMap = new HashMap<>();
    
    private UUID id;
    private boolean authorized = true;
    private GameType originalGameType;
    private PlayerStatus status;
    private boolean previouslyOpped;
    private int voteCount;
    private boolean isVoted;

    private BaseGamemode gamemode;


    public UUID UUID () {
        return id;
    }

    public boolean PreviouslyOpped() {
        return previouslyOpped;
    }

    public int Votes() {
        return voteCount;
    }

    public PlayerStatus Status() {
        return this.status;
    }

    public BaseGamemode Gamemode() { return gamemode; } 
    public <T extends BaseGamemode> T Gamemode(Class<T> clazz) { 
        if (clazz.isInstance(gamemode)) {
            return clazz.cast(gamemode);
        } else {
            throw new ClassCastException("Gamemode cannot be cast to the specified type.");
        }
    }
    
    public static PlayerContext Instantiate(UUID id, GameType gameType) {
        if (playerContextMap.containsKey(id)) return playerContextMap.get(id);

        PlayerContext playerU = new PlayerContext();
        playerU.id = id;
        playerU.originalGameType = gameType;

        playerContextMap.put(id, playerU);
        return playerU;
    }

    public static PlayerContext Get(UUID id) {
        return playerContextMap.get(id);
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
        playerContextMap.get(id).authorized = false;
    }

    public void SetPreviouslyOpped() {
        previouslyOpped = true;
    }

    public void SetStatus(PlayerStatus status) {
        this.status = status;
    }

    public boolean AlreadyVoted() {
        return isVoted;
    }

    public void AddVote() {
        voteCount += 1;
    }

    public void SetVoted() {
        isVoted = true;
    }
}

