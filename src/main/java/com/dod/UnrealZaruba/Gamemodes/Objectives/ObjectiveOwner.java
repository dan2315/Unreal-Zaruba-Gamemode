package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.util.List;

public abstract class ObjectiveOwner {
    private List<GameObjective> objectives;

    public void addObjective(GameObjective objective){
        objectives.add(objective);
    }
    public void removeObjective(GameObjective objective){
        objectives.remove(objective);
    }

    public abstract void onObjectiveStateChanged(GameObjective objective);
    public abstract void onObjectiveCompleted(GameObjective objective);
} 