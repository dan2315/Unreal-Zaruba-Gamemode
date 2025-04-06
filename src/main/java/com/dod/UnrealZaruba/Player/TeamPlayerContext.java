package com.dod.UnrealZaruba.Player;

import java.util.UUID;

import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.Utils.IResettable;
import net.minecraft.world.level.GameType;

public class TeamPlayerContext extends PlayerContext implements IResettable {
    private TeamContext team;
    private boolean tentChosen;
    private String selectedClassId = "soldier";

    public TeamContext Team() { return team; }
    public void SetTeam(TeamContext team) { this.team = team; }
    public boolean TentChosen() { return tentChosen; }
    public void SelectTent(boolean tentChosen) { this.tentChosen = tentChosen; }
    
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
        tentChosen = false;
    }
}
