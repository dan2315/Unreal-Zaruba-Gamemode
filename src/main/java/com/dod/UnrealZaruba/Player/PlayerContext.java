package com.dod.UnrealZaruba.Player;

import java.util.UUID;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import java.util.HashMap;

import net.minecraft.world.level.GameType;

/**
 * <p>Хранитель данных игроков.</p>
 * <li>{@link UUID} id;</li>
 * <li>{@link Boolean} authorized;</li>
 * <li>{@link PlayerStatus} status;</li>
 * <li>{@link GameType} originalGameType;</li>
 * <li>{@link Integer} voteCount;</li>
 * <li>{@link Boolean} isVoted;</li>
 * <li>{@link BaseGamemode} gamemode;</li>
 */
public class PlayerContext {

    public static HashMap<UUID, PlayerContext> playerContextMap = new HashMap<>();
    
    protected UUID id;
    protected GameType originalGameType;
    protected PlayerStatus status;
    protected int voteCount;
    protected boolean isVoted;
    protected boolean isDead;
    protected boolean isReady;

    public boolean sosal;

    protected BaseGamemode gamemode;


    public UUID UUID () {
        return id;
    }

    public int Votes() {
        return voteCount;
    }

    public PlayerStatus Status() {
        return this.status;
    }

    public boolean IsDead() {
        return isDead;
    }

    public boolean IsReady() {
        return isReady;
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

    public void SetReady(boolean ready) {
        isReady = ready;
    }
}

