package com.dod.unrealzaruba.Gamemodes.StartCondition;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.Consumer;

/**
 * A condition that is met after a specified time has passed
 */
public class TimePassedCondition extends Condition implements IDelayedCondition {
    private final int requiredDurationSeconds;
    private final int requiredDurationTicks;
    private Consumer<Integer> onEverySecond;
    private java.util.function.BooleanSupplier readiness;
    private int sustainedTicks = 0;


    /**
     * Creates a new time-based condition
     * 
     * @param requiredDurationSeconds Duration in seconds before condition is met
     */
    public TimePassedCondition(int requiredDurationSeconds) {
        this.requiredDurationSeconds = requiredDurationSeconds;
        this.requiredDurationTicks = requiredDurationSeconds * 20; // Convert seconds to ticks (20 ticks per second)
    }

    @Override
    public boolean isMet() {
        return conditionMet;
    }

    @Override
    public void ResetCondition() {
        sustainedTicks = 0;
        conditionMet = false;
    }

    @Override
    public void Update() {
        if (conditionMet) return;
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        sustainedTicks++;
        
        if (sustainedTicks % 20 == 0) { // Once per second
            int remainingSeconds = Math.max(0, (requiredDurationTicks - sustainedTicks) / 20);
            if (onEverySecond != null) onEverySecond.accept(remainingSeconds);
        }
        
        if (sustainedTicks >= requiredDurationTicks && (readiness == null || readiness.getAsBoolean())) {
            conditionMet = true;

            if (onConditionMet != null) {
                onConditionMet.run();
            }
        }
    }

    @Override
    public int getSustainedTicks() {
        return sustainedTicks;
    }

    /** After the time is up, keeps waiting (at 0) until this is true, e.g. until the map has loaded. */
    public TimePassedCondition WaitFor(java.util.function.BooleanSupplier readiness) {
        this.readiness = readiness;
        return this;
    }

    public TimePassedCondition OnEverySecond(Consumer<Integer> onEverySecond) {
        this.onEverySecond = onEverySecond;
        return this;
    }

    public int getRequiredDurationTicks() {
        return requiredDurationTicks;
    }
    
    public int getRequiredDurationSeconds() {
        return requiredDurationSeconds;
    }
} 