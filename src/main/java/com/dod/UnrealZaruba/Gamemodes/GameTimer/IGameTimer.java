package com.dod.UnrealZaruba.Gamemodes.GameTimer;

public interface IGameTimer {
    void setupScoreboard();
    void update(int seconds, int minutes, boolean isVisible);
    void resetScoreboard();
}