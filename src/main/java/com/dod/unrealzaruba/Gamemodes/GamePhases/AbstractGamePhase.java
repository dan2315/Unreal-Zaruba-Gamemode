package com.dod.unrealzaruba.Gamemodes.GamePhases;

public abstract class AbstractGamePhase {
    public PhaseId phaseId;

    public AbstractGamePhase(PhaseId phaseId) {
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
    public abstract void Clear();
}   
