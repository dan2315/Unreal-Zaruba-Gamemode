package com.dod.UnrealZaruba.Gamemodes.GamePhases;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompositePhase extends AbstractGamePhase implements IPhaseHolder {
    public CompositePhase(PhaseId phaseId) {
        super(phaseId);
    }
    
    protected List<AbstractGamePhase> phases = new ArrayList<>();
    protected AbstractGamePhase currentPhase;
    protected int currentPhaseIndex = 0;

    private final int repeatCount = 1;
    private int currentRepetition = 0;
    
    @Override
    public IPhaseHolder AddPhase(AbstractGamePhase phase) {
        phases.add(phase);
        return this;
    }

    @Override
    public AbstractGamePhase Build() {
        return this;
    }

    @Override
    public void CompletePhase() {
        if (currentPhaseIndex < phases.size() - 1) {
            currentPhaseIndex++;
            currentPhase = phases.get(currentPhaseIndex);
        }
    }

    @Override
    public void CompletePhase(PhaseId phaseId) {
        if (currentPhaseIndex < phases.size() - 1) {
            currentPhaseIndex++;
            var nextPhase = phases.get(currentPhaseIndex);
            if (nextPhase.GetPhaseId() == phaseId) {
                currentPhase = nextPhase;
            } else {
                throw new RuntimeException("Expected phase " + phaseId + " but got " + nextPhase.GetPhaseId());
            }
        }
    }

    @Override
    public void ProceedToPhaseForced(PhaseId phaseId) {
        var phase = GetPhaseById(phaseId);
        if (phase.isPresent()) {
            currentPhaseIndex = phases.indexOf(phase.get());
            currentPhase = phase.get();
        }
    }

    @Override
    public Optional<AbstractGamePhase> GetPhaseById(PhaseId phaseId) {
        return phases.stream()
                .filter(phase -> phase.GetPhaseId() == phaseId)
                .findFirst();
    }

    @Override
    public void OnStart() {
        currentPhase.OnStart();
    }

    @Override
    public void OnCompleted() {
        currentPhase.OnCompleted();
        currentRepetition++;
        if (currentRepetition < repeatCount) {
            currentPhaseIndex = 0;
            currentPhase = phases.get(currentPhaseIndex);
            currentPhase.OnStart();
        }
    }

    @Override
    public void OnTick(int ticks) {
        currentPhase.OnTick(ticks);
    }
    
}
