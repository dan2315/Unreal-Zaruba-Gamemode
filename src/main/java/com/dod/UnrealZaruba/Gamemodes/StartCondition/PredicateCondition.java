package com.dod.UnrealZaruba.Gamemodes.StartCondition;

import java.util.function.BooleanSupplier;

public class PredicateCondition extends Condition {
    BooleanSupplier predicate;

    public PredicateCondition(BooleanSupplier predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean isMet() {
        return predicate.getAsBoolean();
    }

    public void Test() {

    }

    @Override
    public void ResetCondition() {

    }
}
