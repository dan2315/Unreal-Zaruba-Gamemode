package com.dod.UnrealZaruba.Gamemodes.StartCondition;

import net.minecraftforge.server.ServerLifecycleHooks;

public class EnoughPlayersCondition extends StartCondition {
    private final int requiredPlayers;

    public EnoughPlayersCondition(int requiredPlayers) {
        this.requiredPlayers = requiredPlayers;
    }


    @Override
    public boolean isMet() {
        return ServerLifecycleHooks.getCurrentServer().getPlayerCount() >= requiredPlayers;
    }
}