package com.dod.UnrealZaruba.TeamLogic;

import java.util.HashMap;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;

/**
 * Team data configuration class.
 * Contains mapping of team colors to their data entries.
 */
public class TeamData {
    private HashMap<TeamColor, TeamDataEntry> teamSpawns = new HashMap<>();
    
    /**
     * Gets the team spawns map
     * 
     * @return The team spawns map
     */
    public HashMap<TeamColor, TeamDataEntry> getTeamSpawns() {
        return teamSpawns;
    }
    
    /**
     * Sets the team spawns map
     * 
     * @param teamSpawns The team spawns map to set
     */
    public void setTeamSpawns(HashMap<TeamColor, TeamDataEntry> teamSpawns) {
        this.teamSpawns = teamSpawns;
    }
}


