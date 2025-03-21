package com.dod.UnrealZaruba.Gamemodes.StartCondition;

import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * A start condition that requires a minimum number of players to be present
 * for a sustained period of time before triggering.
 */
public class SustainedPlayerCountCondition extends StartCondition {
    private final int requiredPlayerCount;
    private final int requiredDurationTicks;
    private int sustainedTicks = 0;
    private boolean conditionMet = false;
    private Runnable onConditionMet;

    /**
     * Creates a new sustained player count condition
     * 
     * @param requiredPlayerCount The minimum number of players required
     * @param requiredDurationSeconds The duration in seconds that the player count must be maintained
     */
    public SustainedPlayerCountCondition(int requiredPlayerCount, int requiredDurationSeconds) {
        this.requiredPlayerCount = requiredPlayerCount;
        this.requiredDurationTicks = requiredDurationSeconds * 20; // Convert seconds to ticks (20 ticks per second)
    }

    @Override
    public boolean isMet() {
        return conditionMet;
    }

    @Override
    public void Update() {
        if (conditionMet) return;
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        int currentPlayerCount = server.getPlayerList().getPlayerCount();
        
        if (currentPlayerCount >= requiredPlayerCount) {
            sustainedTicks++;
            
            // Log progress at regular intervals
            if (sustainedTicks % 20 == 0) { // Every second
                int remainingSeconds = (requiredDurationTicks - sustainedTicks) / 20;
                UnrealZaruba.LOGGER.info("Player count condition: " + currentPlayerCount + 
                                      "/" + requiredPlayerCount + " players, " +
                                      remainingSeconds + " seconds remaining");
            }
            
            if (sustainedTicks >= requiredDurationTicks) {
                conditionMet = true;
                UnrealZaruba.LOGGER.info("Player count condition met: " + currentPlayerCount + 
                                      "/" + requiredPlayerCount + " players for " +
                                      (requiredDurationTicks / 20) + " seconds");
                if (onConditionMet != null) {
                    onConditionMet.run();
                }
            }
        } else {
            // Reset the counter if player count drops below threshold
            if (sustainedTicks > 0) {
                UnrealZaruba.LOGGER.info("Player count dropped to " + currentPlayerCount + 
                                      "/" + requiredPlayerCount + ", resetting timer");
                sustainedTicks = 0;
            }
        }
    }

    @Override
    public void SetOnConditionMet(Runnable onConditionMet) {
        this.onConditionMet = onConditionMet;
    }

    /**
     * Gets the current sustained duration in ticks
     */
    public int getSustainedTicks() {
        return sustainedTicks;
    }
    
    /**
     * Gets the required duration in ticks
     */
    public int getRequiredDurationTicks() {
        return requiredDurationTicks;
    }
    
    public int getRequiredPlayerCount() {
        return requiredPlayerCount;
    }


} 