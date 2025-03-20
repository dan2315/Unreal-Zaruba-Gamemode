package com.dod.UnrealZaruba.Gamemodes.StartCondition;


public abstract class StartCondition {
    private Runnable onConditionMet;

    public void SetOnConditionMet(Runnable onConditionMet) {
        this.onConditionMet = onConditionMet;
    }   

    public abstract boolean isMet();

    public void Update() {
        if (isMet()) {
            onConditionMet.run();
        }
    }
}
