package com.dod.UnrealZaruba.Gamemodes.Objectives;

public interface IObjectiveNotifier {

    void onObjectiveStateChanged(GameObjective objective);

    void onObjectiveCompleted(GameObjective objective);
} 