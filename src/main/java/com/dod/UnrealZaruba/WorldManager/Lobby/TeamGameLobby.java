package com.dod.UnrealZaruba.WorldManager.Lobby;

import java.util.function.Supplier;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class TeamGameLobby extends Lobby {

    TeamManager teamManager;

    public TeamGameLobby(Supplier<BaseGamemode> gamemodeFactory, Integer maxPlayerCount, ResourceKey<Level> dimension,
            ResourceKey<Level> gameDimension) {
        super(gamemodeFactory, maxPlayerCount, dimension, gameDimension);
        teamManager = new TeamManager();
    }

    @Override
    public void StartGame() {
        super.StartGame();
        
    }
    
}
