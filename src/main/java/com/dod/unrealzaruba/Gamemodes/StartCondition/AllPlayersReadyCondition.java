package com.dod.unrealzaruba.Gamemodes.StartCondition;

import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Title.TitleMessage;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

public class AllPlayersReadyCondition extends Condition implements IDelayedCondition {
    private final int requiredDurationTicks;
    private int sustainedTicks = 0;
    private final int minimumPlayers;

    public AllPlayersReadyCondition(int sustainedDurationSeconds) {
        this(sustainedDurationSeconds, 1);
    }

    public AllPlayersReadyCondition(int sustainedDurationSeconds, int minimumPlayers) {
        this.requiredDurationTicks = sustainedDurationSeconds * 20; // Convert seconds to ticks (20 ticks per second)
        this.minimumPlayers = minimumPlayers;
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
        
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        
        if (players.size() < minimumPlayers) {
            if (sustainedTicks > 0) {
                UnrealZaruba.LOGGER.info("Not enough players to start. Need at least " + minimumPlayers);
                for (ServerPlayer player : players) {
                    TitleMessage.sendActionbar(player, Component.literal(
                        "Not enough players to start. Need at least " + minimumPlayers
                    ));
                }
                sustainedTicks = 0;
            }
            return;
        }
        
        boolean allPlayersReady = true;
        List<String> notReadyPlayers = new ArrayList<>();
        
        for (ServerPlayer player : players) {
            PlayerContext playerContext = PlayerContext.Get(player.getUUID());
            if (playerContext == null || !playerContext.IsReady()) {
                allPlayersReady = false;
                notReadyPlayers.add(player.getName().getString());
            }
        }
        
        if (allPlayersReady) {
            sustainedTicks++;
            if (sustainedTicks % 20 == 0) { // Every second
                int remainingSeconds = (requiredDurationTicks - sustainedTicks) / 20;
                UnrealZaruba.LOGGER.info("All players ready condition: " +
                                      remainingSeconds + " seconds remaining");
                
                for (ServerPlayer player : players) {
                    TitleMessage.sendActionbar(player, Component.literal(
                        "Starting in " + remainingSeconds + " seconds (all players ready)"
                    ));
                }
            }
            
            if (sustainedTicks >= requiredDurationTicks) {
                conditionMet = true;
                UnrealZaruba.LOGGER.info("All players ready condition met: maintained for " +
                                      (requiredDurationTicks / 20) + " seconds");
                
                for (ServerPlayer player : players) {
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
                UnrealZaruba.LOGGER.info("Not all players are ready, resetting timer");
                
                StringBuilder statusMessage = new StringBuilder("Waiting for players: ");
                for (String playerName : notReadyPlayers) {
                    statusMessage.append(playerName).append(", ");
                }
                // Remove the trailing comma and space
                if (notReadyPlayers.size() > 0) {
                    statusMessage.setLength(statusMessage.length() - 2);
                }
                
                for (ServerPlayer player : players) {
                    TitleMessage.sendActionbar(player, Component.literal(statusMessage.toString()));
                }
                
                sustainedTicks = 0;
            }
        }
    }
    
    public int getSustainedTicks() {
        return sustainedTicks;
    }
    
    public int getRequiredDurationTicks() {
        return requiredDurationTicks;
    }
} 