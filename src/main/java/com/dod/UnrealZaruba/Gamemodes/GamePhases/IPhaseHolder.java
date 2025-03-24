package com.dod.UnrealZaruba.Gamemodes.GamePhases;

import java.util.Optional;

public interface IPhaseHolder {
    public IPhaseHolder AddPhase(AbstractGamePhase phase);
    public AbstractGamePhase Build();
    public void ProceedToNextPhase();
    public void ProceedToNextPhase(PhaseId phaseId);
    public void ProceedToPhaseForced(PhaseId phaseId);
    public Optional<AbstractGamePhase> GetPhaseById(PhaseId phaseId);
}

