package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.util.List;

public class ObjectivesHandler {
    List<GameObjective> objectives;

    public void addObjective(GameObjective objective) {
        objectives.add(objective);
    }

    public void removeObjective(GameObjective objective) {
        objectives.remove(objective);
    }

    public void updateObjectives() {
        objectives.forEach(objective -> objective.Update());
    }
}
