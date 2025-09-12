package com.dod.UnrealZaruba.Utils.Timers;

/**
 * Common interface for all timer implementations
 */
public interface ITimer {

    void start();
    void update();
    void pause();
    void resume();
    void reset();
    void dispose(boolean complete);
    long getDuration();
    float getProgress();
    long getRemainingTime();
    long getElapsedTime();
    boolean isRunning();
    boolean isCompleted();
} 