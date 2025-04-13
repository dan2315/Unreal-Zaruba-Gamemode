package com.dod.UnrealZaruba.Gamemodes.GameTimer;

public interface IGameTimer {
    void setup();
    void update(int seconds, int minutes, boolean isVisible);
    void reset();
}