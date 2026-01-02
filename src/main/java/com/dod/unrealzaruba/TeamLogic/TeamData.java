package com.dod.unrealzaruba.TeamLogic;

import java.util.HashMap;
import java.util.Map;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.AbstractGamemodeData;

/**
 * Team data configuration class.
 * Contains mapping of team colors to their data entries.
 * Integrates the handler functionality directly.
 */
public class TeamData extends AbstractGamemodeData<TeamData.TeamDataPayload> {
    private static final String DATA_NAME = "teamData";

    public TeamData(Class<? extends BaseGamemode> gamemodeClass) {
        super(TeamDataPayload.class, gamemodeClass, DATA_NAME, new TeamDataPayload());
    }
    
    @Override
    public Class<TeamDataPayload> getDataClass() {
        return TeamDataPayload.class;
    }

    public TeamDataEntry getTeamData(TeamColor teamColor) {
        return data.getTeam(teamColor);
    }

    public void setTeamData(TeamColor teamColor, TeamDataEntry entry) {
        data.addTeam(teamColor, entry);
        try {
            saveData();
        } catch (Exception e) {
            // Already logged in AbstractGamemodeData
        }
    }

    public boolean removeTeamData(TeamColor teamColor) {
        boolean result = data.removeTeam(teamColor);
        if (result) {
            try {
                saveData();
            } catch (Exception e) {
                // Already logged in AbstractGamemodeData
            }
        }
        return result;
    }

    public Map<TeamColor, TeamDataEntry> getTeams() {
        return data.getAllTeams();
    }

    public boolean hasTeam(TeamColor teamColor) {
        return data.hasTeam(teamColor);
    }
    
    /**
     * Data payload class for team data.
     * Contains all team definitions with their spawn points and other properties.
     */
    public static class TeamDataPayload {
        // Map of team color to team data entry
        private HashMap<TeamColor, TeamDataEntry> teams = new HashMap<>();
        
        /**
         * Default constructor for serialization
         */
        public TeamDataPayload() {
            // Empty constructor for GSON
        }

        public TeamDataEntry getTeam(TeamColor teamColor) {
            return teams.get(teamColor);
        }
        
        /**
         * Add or update a team
         * 
         * @param teamColor The team color
         * @param entry The team data entry
         */
        public void addTeam(TeamColor teamColor, TeamDataEntry entry) {
            teams.put(teamColor, entry);
        }
        
        /**
         * Remove a team
         * 
         * @param teamColor The team color
         * @return True if removed, false if not found
         */
        public boolean removeTeam(TeamColor teamColor) {
            return teams.remove(teamColor) != null;
        }
        
        /**
         * Check if a team exists
         * 
         * @param teamColor The team color
         * @return True if the team exists, false otherwise
         */
        public boolean hasTeam(TeamColor teamColor) {
            return teams.containsKey(teamColor);
        }
        
        /**
         * Get all teams
         * 
         * @return Map of teams by color
         */
        public Map<TeamColor, TeamDataEntry> getAllTeams() {
            return teams;
        }

        /**
         * For serialization
         */
        public HashMap<TeamColor, TeamDataEntry> getTeams() {
            return teams;
        }

        /**
         * For deserialization
         */
        public void setTeams(HashMap<TeamColor, TeamDataEntry> teams) {
            this.teams = teams;
        }

        @Override
        public String toString() {
            return "TeamDataPayload{" +
                    "teams=" + teams +
                    '}';
        }
    }
}


