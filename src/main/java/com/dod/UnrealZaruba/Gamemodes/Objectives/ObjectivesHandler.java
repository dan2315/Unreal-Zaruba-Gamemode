package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.util.List;

public class ObjectivesHandler {
    List<GameObjective> objectives;

    protected Runnable onCompleted;

    public void OnObjectivesCompleted(Runnable onCompleted) {
        this.onCompleted = onCompleted;
    }

    public void addObjective(GameObjective objective) {
        objectives.add(objective);
    }

    public void removeObjective(GameObjective objective) {
        objectives.remove(objective);
    }

    public void onServerTick() {
        objectives.forEach(objective -> objective.Update());
    }
}
