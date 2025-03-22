package com.dod.UnrealZaruba.Gamemodes.StartCondition;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.network.chat.Component;
import com.dod.UnrealZaruba.Title.TitleMessage;

import java.util.HashMap;
import java.util.Map;


public class SustainedPlayerCountCondition extends StartCondition {
    private final int requiredPlayersPerTeam;
    private final int requiredDurationTicks;
    private int sustainedTicks = 0;
    private boolean conditionMet = false;
    private Runnable onConditionMet;
    private final TeamManager teamManager;
    private final HashMap<TeamColor, Boolean> teamReadyStatus = new HashMap<>();

    public SustainedPlayerCountCondition(TeamManager teamManager, int requiredPlayersPerTeam, int requiredDurationSeconds) {
        this.teamManager = teamManager;
        this.requiredPlayersPerTeam = requiredPlayersPerTeam;
        this.requiredDurationTicks = requiredDurationSeconds * 20; // Convert seconds to ticks (20 ticks per second)
    }

    @Override
    public boolean isMet() {
        return conditionMet;
    }

    @Override
    public void Update() {
        if (conditionMet) return;
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        teamReadyStatus.clear();
        
        boolean allTeamsReady = true;
        int teamsCount = 0;
        
        for (Map.Entry<TeamColor, TeamContext> entry : teamManager.GetTeams().entrySet()) {
            TeamColor teamColor = entry.getKey();
            TeamContext team = entry.getValue();
            int teamPlayerCount = team.MembersCount();
            
            boolean isTeamReady = teamPlayerCount >= requiredPlayersPerTeam;
            teamReadyStatus.put(teamColor, isTeamReady);
            
            if (!isTeamReady) {
                allTeamsReady = false;
            }
            
            teamsCount++;
        }
        
        if (teamsCount < 2) {
            allTeamsReady = false;
        }
        
        if (allTeamsReady) {
            sustainedTicks++;
            if (sustainedTicks % 20 == 0) {
                int remainingSeconds = (requiredDurationTicks - sustainedTicks) / 20;
                UnrealZaruba.LOGGER.info("Team player count condition: all teams have at least " + 
                                      requiredPlayersPerTeam + " players, " +
                                      remainingSeconds + " seconds remaining");
                
                for (var player : server.getPlayerList().getPlayers()) {
                    TitleMessage.sendActionbar(player, Component.literal(
                        "Starting in " + remainingSeconds + " seconds (all teams ready)"
                    ));
                }
            }
            
            if (sustainedTicks >= requiredDurationTicks) {
                conditionMet = true;
                UnrealZaruba.LOGGER.info("Team player count condition met: all teams have at least " + 
                                      requiredPlayersPerTeam + " players for " +
                                      (requiredDurationTicks / 20) + " seconds");
                
                for (var player : server.getPlayerList().getPlayers()) {
                    TitleMessage.showTitle(player, 
                        Component.literal("Game Starting!"),
                        Component.literal("Good luck!"),
                        60
                    );
                }
                
                if (onConditionMet != null) {
                    onConditionMet.run();
                }
            }
        } else {
            if (sustainedTicks > 0) {
                UnrealZaruba.LOGGER.info("Team player count dropped below required level, resetting timer");
                
                StringBuilder statusMessage = new StringBuilder("Not enough players! Need ");
                statusMessage.append(requiredPlayersPerTeam).append(" per team: ");
                
                for (Map.Entry<TeamColor, Boolean> status : teamReadyStatus.entrySet()) {
                    TeamColor teamColor = status.getKey();
                    boolean isReady = status.getValue();
                    TeamContext team = teamManager.GetTeams().get(teamColor);
                    int playerCount = team.MembersCount();
                    
                    statusMessage.append(teamColor.getDisplayName())
                                 .append(" (")
                                 .append(playerCount)
                                 .append("/")
                                 .append(requiredPlayersPerTeam)
                                 .append(") ")
                                 .append(isReady ? "✓" : "✗")
                                 .append(" ");
                }
                
                for (var player : server.getPlayerList().getPlayers()) {
                    TitleMessage.sendActionbar(player, Component.literal(statusMessage.toString()));
                }
                
                sustainedTicks = 0;
            }
        }
    }

    @Override
    public void SetOnConditionMet(Runnable onConditionMet) {
        this.onConditionMet = onConditionMet;
    }

    public int getSustainedTicks() {
        return sustainedTicks;
    }
    
    public int getRequiredDurationTicks() {
        return requiredDurationTicks;
    }
    
    public int getRequiredPlayersPerTeam() {
        return requiredPlayersPerTeam;
    }
} 