package com.dod.UnrealZaruba.Gamemodes.StartCondition;

import com.dod.UnrealZaruba.UnrealZaruba;
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
