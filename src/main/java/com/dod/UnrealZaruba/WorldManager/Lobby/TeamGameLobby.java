package com.dod.UnrealZaruba.WorldManager.Lobby;

import com.dod.UnrealZaruba.TeamLogic.TeamManager;


public class TeamGameLobby extends Lobby {

    TeamManager teamManager;

    public TeamGameLobby() {
        teamManager = new TeamManager();
    }

    @Override
    public void StartGame() {
        super.StartGame();
        
    }
    
}
