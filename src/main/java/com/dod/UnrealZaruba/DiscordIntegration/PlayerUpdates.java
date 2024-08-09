package com.dod.UnrealZaruba.DiscordIntegration;

import java.util.List;
import java.util.UUID;

public class PlayerUpdates {
    private List<UUID> playersWhoWon;
    private List<UUID> playersWhoLost;

    public PlayerUpdates(List<UUID> playersWhoWon, List<UUID> playersWhoLost) {
        this.playersWhoWon = playersWhoWon;
        this.playersWhoLost = playersWhoLost;
    }

    public List<UUID> getPlayersWhoWon() {
        return playersWhoWon;
    }

    public void setPlayersWhoWon(List<UUID> playersWhoWon) {
        this.playersWhoWon = playersWhoWon;
    }

    public List<UUID> getPlayersWhoLost() {
        return playersWhoLost;
    }

    public void setPlayersWhoLost(List<UUID> playersWhoLost) {
        this.playersWhoLost = playersWhoLost;
    }
}