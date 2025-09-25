package com.dod.unrealzaruba.Gamemodes.StartCondition;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class CombinedOrCondition extends Condition implements IDelayedCondition {
    private final List<Condition> conditions;

    public CombinedOrCondition(Condition... conditions) {
        this.conditions = new ArrayList<>(Arrays.asList(conditions));
    }

    public CombinedOrCondition(List<Condition> conditions) {
        this.conditions = new ArrayList<>(conditions);
    }
    
    @Override
    public boolean isMet() {
        if (conditionMet) return true;
        
        for (Condition condition : conditions) {
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
        
        for (Condition condition : conditions) {
            condition.ResetCondition();
        }
    }
    
    @Override
    public void Update() {
        if (conditionMet) return;
        
        for (Condition condition : conditions) {
            condition.Update();
        }
        
        if (isMet() && onConditionMet != null) {
            onConditionMet.run();
        }
    }
    
    public void addCondition(Condition condition) {
        conditions.add(condition);
    }
    
    public boolean removeCondition(Condition condition) {
        return conditions.remove(condition);
    }

    @Override
    public int getSustainedTicks() {
        int minTicks = Integer.MAX_VALUE;
        for (Condition condition : conditions) {
            if (condition instanceof IDelayedCondition delayedCondition) {
                minTicks = Math.min(minTicks, delayedCondition.getSustainedTicks());
            }
        }
        return minTicks;
    }
    
} 