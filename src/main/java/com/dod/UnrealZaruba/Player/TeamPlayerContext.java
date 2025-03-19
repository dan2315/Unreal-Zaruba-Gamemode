package com.dod.UnrealZaruba.Player;

import com.dod.UnrealZaruba.TeamLogic.TeamContext;

public class TeamPlayerContext extends PlayerContext {
    private TeamContext team;
    private boolean tentChosen;

    public TeamContext Team() { return team; }
    public void SetTeam(TeamContext team) { this.team = team; }
    public boolean TentChosen() { return tentChosen; }
    public void SelectTent(boolean tentChosen) { this.tentChosen = tentChosen; }
}
