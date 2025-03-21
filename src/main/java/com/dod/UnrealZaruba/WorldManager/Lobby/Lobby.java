package com.dod.UnrealZaruba.WorldManager.Lobby;

import com.dod.UnrealZaruba.Player.PlayerStatus;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Player.PlayerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class Lobby {
    private List<UUID> players = new ArrayList<>();
    private Supplier<BaseGamemode> gamemodeFactory;
    private Integer maxPlayerCount;
    private Integer playerCount;

    public Lobby() {

    }

    public void Enter(PlayerContext playerContext) {
        playerContext.SetStatus(PlayerStatus.InLobby);
        playerCount++;
        players.add(playerContext.UUID());
        if (playerCount >= maxPlayerCount) {
            StartGame();
        }
    }

    public void Leave(PlayerContext playerContext) {
        playerContext.SetStatus(PlayerStatus.InGame);
        players.remove(playerContext.UUID());
        playerCount--;
    }

    public void StartGame() {
        BaseGamemode gamemode = gamemodeFactory.get();
    }
}
