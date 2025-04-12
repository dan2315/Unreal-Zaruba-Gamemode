package com.dod.UnrealZaruba.Gamemodes.StartCondition;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class CombinedOrCondition extends StartCondition implements IDelayedCondition {
    private final List<StartCondition> conditions;

    public CombinedOrCondition(StartCondition... conditions) {
        this.conditions = new ArrayList<>(Arrays.asList(conditions));
    }

    public CombinedOrCondition(List<StartCondition> conditions) {
        this.conditions = new ArrayList<>(conditions);
    }
    
    @Override
    public boolean isMet() {
        if (conditionMet) return true;
        
        for (StartCondition condition : conditions) {
            if (condition.isMet()) {
                conditionMet = true;
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void ResetCondition() {
        conditionMet = false;
        
        for (StartCondition condition : conditions) {
            condition.ResetCondition();
        }
    }
    
    @Override
    public void Update() {
        if (conditionMet) return;
        
        for (StartCondition condition : conditions) {
            condition.Update();
        }
        
        if (isMet() && onConditionMet != null) {
            onConditionMet.run();
        }
    }
    
    public void addCondition(StartCondition condition) {
        conditions.add(condition);
    }
    
    public boolean removeCondition(StartCondition condition) {
        return conditions.remove(condition);
    }

    @Override
    public int getSustainedTicks() {
        int minTicks = Integer.MAX_VALUE;
        for (StartCondition condition : conditions) {
            if (condition instanceof IDelayedCondition delayedCondition) {
                minTicks = Math.min(minTicks, delayedCondition.getSustainedTicks());
            }
        }
        return minTicks;
    }
    
} 