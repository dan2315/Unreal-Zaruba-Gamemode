package com.dod.UnrealZaruba.Gamemodes.GamePhases;

public class GamePhase extends AbstractGamePhase {
    public Runnable onStart;
    public Runnable onCompleted;

    public GamePhase(PhaseId phaseId, Runnable onStart, Runnable onCompleted) {
        super(phaseId);
        this.onStart = onStart;
        this.onCompleted = onCompleted;
    }

    public String GetName() {
        return phaseId.getPhaseName();
    }

    public void OnStart() {
        onStart.run();
    }

    public void OnCompleted() { 
        onCompleted.run();
    }

    @Override
    public void OnTick(int ticks) {
    }
    
    
}
