package com.dod.unrealzaruba.Player;

import java.util.UUID;

import com.dod.unrealzaruba.Gamemodes.RespawnPoints.IRespawnPoint;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.utils.IResettable;
import net.minecraft.world.level.GameType;

public class TeamPlayerContext extends PlayerContext implements IResettable {
    private TeamContext team;
    private IRespawnPoint selectedRespawnPoint;
    private String selectedClassId = "soldier";

    public TeamContext Team() { return team; }
    public void SetTeam(TeamContext team) { this.team = team; }
    public boolean RespawnPointSelected() { return selectedRespawnPoint != null; }
    public void SelectRespawnPoint(IRespawnPoint respawnPoint) { this.selectedRespawnPoint = respawnPoint; }
    public void DeselectRespawnPoint() { selectedRespawnPoint = null; }
    public IRespawnPoint RespawnPoint() { return selectedRespawnPoint; }
    
    public String SelectedClassId() { return selectedClassId; }
    public void SetSelectedClassId(String classId) { this.selectedClassId = classId; }

    public static TeamPlayerContext Instantiate(UUID id, GameType gameType) {
        if (playerContextMap.containsKey(id)) return (TeamPlayerContext) playerContextMap.get(id);

        TeamPlayerContext playerContext = new TeamPlayerContext();
        playerContext.id = id;
        playerContext.originalGameType = gameType;

        playerContextMap.put(id, playerContext);
        return playerContext;
    }

    @Override
    public void reset() {
        team = null;
        selectedRespawnPoint = null;
        isReady = false;
    }
}
