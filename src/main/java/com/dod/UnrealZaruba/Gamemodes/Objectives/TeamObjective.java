package com.dod.UnrealZaruba.Gamemodes.Objectives;

public class TeamObjective extends GameObjective {

    @Override
    public Boolean IsCompleted() {
        return false;
    }

    @Override
    protected boolean UpdateImplementation() {
        return false;
    }

    @Override
    protected void OnCompleted() {
    }
    
}
