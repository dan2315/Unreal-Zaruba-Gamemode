package com.dod.UnrealZaruba.Gamemodes.StartCondition;

public abstract class StartCondition {
    protected Runnable onConditionMet;
    protected boolean conditionMet = false;

    public void SetOnConditionMet(Runnable onConditionMet) {
        this.onConditionMet = onConditionMet;
    }   

    // It's responsible for both returning if the condition is met and setting the conditionMet flag
    public abstract boolean isMet(); 

    public abstract void ResetCondition();

    public void Update() {
        if (conditionMet) return;
        if (isMet()) {
            onConditionMet.run();
        }
    }
}
