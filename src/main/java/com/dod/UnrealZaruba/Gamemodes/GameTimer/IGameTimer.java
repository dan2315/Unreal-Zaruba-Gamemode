package com.dod.unrealzaruba.Gamemodes.GameTimer;

public interface IGameTimer {
    void startCountDown(long startTime, int durationSeconds);
    void stop();
}