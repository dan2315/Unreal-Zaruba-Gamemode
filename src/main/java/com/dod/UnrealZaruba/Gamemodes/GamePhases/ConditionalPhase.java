package com.dod.UnrealZaruba.Gamemodes.GamePhases;

import com.dod.UnrealZaruba.Gamemodes.StartCondition.Condition;

public class ConditionalPhase extends AbstractGamePhase {
    private Condition condition;
    private Runnable OnStart;
    private Runnable OnCompleted;

    public ConditionalPhase(PhaseId phaseId, Runnable OnStart, Runnable OnCompleted, Condition condition) {
        super(phaseId);
        this.condition = condition;
        this.OnStart = OnStart;
        this.OnCompleted = OnCompleted;
    }

    public void SetOnConditionMet(Runnable onConditionMet) {
        condition.SetOnConditionMet(onConditionMet);
    }

    public void OnTick(int ticks) {
        condition.Update();
    }

    @Override
    public void OnStart() {
        OnStart.run();
    }

    @Override
    public void OnCompleted() {
        OnCompleted.run();
    }

    @Override
    public void Clear() {
    }
}