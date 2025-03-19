package com.dod.UnrealZaruba.WorldManager.Lobby;

import com.dod.UnrealZaruba.Player.PlayerStatus;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class Lobby {

    private final ResourceKey<Level> dimension;
    private final ResourceKey<Level> gameDimension;

    private List<UUID> players = new ArrayList<>();
    private Supplier<BaseGamemode> gamemodeFactory;
    private Integer maxPlayerCount;
    private Integer playerCount;

    public Lobby(Supplier<BaseGamemode> gamemodeFactory, Integer maxPlayerCount, ResourceKey<Level> dimension, ResourceKey<Level> gameDimension) {
        this.dimension = dimension;
        this.gameDimension = gameDimension;
        this.maxPlayerCount = maxPlayerCount;
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
        gamemode.TeleportPlayersInGame(gameDimension);
    }

    public ResourceKey<Level> GetDimension() {
        return dimension;
    }

    public ResourceKey<Level> GetGameDimension() {
        return gameDimension;
    }
}
