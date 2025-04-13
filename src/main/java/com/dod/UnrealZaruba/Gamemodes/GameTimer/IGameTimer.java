package com.dod.UnrealZaruba.Gamemodes.GameTimer;

public interface IGameTimer {
    void setupScoreboard();
    void updateMinutes(int minutes);
    void updateSeconds(int seconds);
    void resetScoreboard();
}