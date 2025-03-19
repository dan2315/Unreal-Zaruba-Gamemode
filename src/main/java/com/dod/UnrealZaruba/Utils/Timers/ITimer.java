package com.dod.UnrealZaruba.Utils.Timers;

/**
 * Common interface for all timer implementations
 */
public interface ITimer {
    /**
     * Starts the timer
     */
    void start();
    
    /**
     * Updates the timer state
     */
    void update();
    
    /**
     * Pauses the timer
     */
    void pause();
    
    /**
     * Resumes the timer after pause
     */
    void resume();
    
    /**
     * Resets the timer to initial state
     */
    void reset();
    
    /**
     * Disposes the timer
     * @param complete Whether to trigger completion callback
     */
    void dispose(boolean complete);
    
    /**
     * Gets the current progress of the timer (0.0 to 1.0)
     * @return Progress as a float between 0 and 1
     */
    float getProgress();
    
    /**
     * Gets the remaining time in milliseconds
     * @return Remaining time in ms
     */
    long getRemainingTime();
    
    /**
     * Gets the elapsed time in milliseconds
     * @return Elapsed time in ms
     */
    long getElapsedTime();
    
    /**
     * Checks if the timer is running
     * @return True if timer is active
     */
    boolean isRunning();
    
    /**
     * Checks if the timer is completed
     * @return True if timer has completed
     */
    boolean isCompleted();
} 