package com.dod.UnrealZaruba.Gamemodes.StartCondition;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Title.TitleMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class EnoughPlayersCondition extends Condition {
    private final int requiredPlayers;
    private final int requiredDurationTicks;
    private int sustainedTicks = 0;

    public EnoughPlayersCondition(int requiredPlayers) {
        this.requiredPlayers = requiredPlayers;
        this.requiredDurationTicks = 20 * 20; // 20 seconds * 20 ticks per second
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
        
        boolean enoughPlayers = server.getPlayerCount() >= requiredPlayers;
        
        if (enoughPlayers) {
            sustainedTicks++;
            if (sustainedTicks % 20 == 0) { // Every second
                int remainingSeconds = (requiredDurationTicks - sustainedTicks) / 20;
                UnrealZaruba.LOGGER.info("Player count condition: " + server.getPlayerCount() + 
                                        "/" + requiredPlayers + " players, " +
                                        remainingSeconds + " seconds remaining");
                
                for (var player : server.getPlayerList().getPlayers()) {
                    TitleMessage.sendActionbar(player, Component.literal(
                        "Starting in " + remainingSeconds + " seconds (" + 
                        server.getPlayerCount() + "/" + requiredPlayers + " players)"
                    ));
                }
            }
            
            if (sustainedTicks >= requiredDurationTicks) {
                conditionMet = true;
                UnrealZaruba.LOGGER.info("Player count condition met: " + 
                                       server.getPlayerCount() + "/" + requiredPlayers + 
                                       " players for " + (requiredDurationTicks / 20) + " seconds");
                
                for (var player : server.getPlayerList().getPlayers()) {
                    TitleMessage.showTitle(player, 
                        Component.literal("Game Starting!"),
                        Component.literal("Good luck!"),
                        60
                    );
                }
                
                if (onConditionMet != null) {
                    onConditionMet.run();
                }
            }
        } else {
            if (sustainedTicks > 0) {
                UnrealZaruba.LOGGER.info("Player count dropped below required level, resetting timer");
                
                for (var player : server.getPlayerList().getPlayers()) {
                    TitleMessage.sendActionbar(player, Component.literal(
                        "Not enough players! Need " + requiredPlayers + 
                        " (currently " + server.getPlayerCount() + ")"
                    ));
                }
                
                sustainedTicks = 0;
            }
        }
    }
}