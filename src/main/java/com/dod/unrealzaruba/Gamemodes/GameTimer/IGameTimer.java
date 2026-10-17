package com.dod.unrealzaruba.Gamemodes.GameTimer;

public interface IGameTimer {
    void startCountDown(long startTime, int durationSeconds);
    void stop();

    /** Tells a player who joined while the countdown runs where it is. */
    default void syncTo(net.minecraft.server.level.ServerPlayer player) {}
}