package com.dod.UnrealZaruba.Gamemodes.StartCondition;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Title.TitleMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * A condition that is met after a specified time has passed
 */
public class TimePassedCondition extends Condition implements IDelayedCondition {
    private final int requiredDurationSeconds;
    private final int requiredDurationTicks;
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
            int remainingSeconds = (requiredDurationTicks - sustainedTicks) / 20;
            UnrealZaruba.LOGGER.info("Time passed condition: " + remainingSeconds + " seconds remaining");
            
            for (var player : server.getPlayerList().getPlayers()) {
                TitleMessage.sendActionbar(player, Component.literal(
                    "Starting in " + remainingSeconds + " seconds"
                ));
            }
        }
        
        if (sustainedTicks >= requiredDurationTicks) {
            conditionMet = true;
            UnrealZaruba.LOGGER.info("Time passed condition met: " + requiredDurationSeconds + " seconds elapsed");
            
            for (var player : server.getPlayerList().getPlayers()) {
                TitleMessage.showTitle(player, 
                    Component.literal("Time's up!"),
                    Component.literal("Starting next phase..."),
                    60
                );
            }
            
            if (onConditionMet != null) {
                onConditionMet.run();
            }
        }
    }

    @Override
    public int getSustainedTicks() {
        return sustainedTicks;
    }
    
    public int getRequiredDurationTicks() {
        return requiredDurationTicks;
    }
    
    public int getRequiredDurationSeconds() {
        return requiredDurationSeconds;
    }
} 