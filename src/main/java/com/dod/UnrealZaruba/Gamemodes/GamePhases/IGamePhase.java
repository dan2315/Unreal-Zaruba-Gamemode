package com.dod.UnrealZaruba.Gamemodes.GamePhases;

public abstract class IGamePhase {
    public PhaseId phaseId;

    public IGamePhase(PhaseId phaseId) {
        this.phaseId = phaseId;
    }

    public String GetName() {
        return phaseId.getPhaseName();
    }
    
    public PhaseId GetPhaseId() {
        return phaseId;
    }

    public abstract void OnStart();
    public abstract void OnCompleted();
    public abstract void OnTick(int ticks);
}   
