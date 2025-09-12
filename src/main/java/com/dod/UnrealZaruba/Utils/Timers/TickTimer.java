package com.dod.UnrealZaruba.Utils.Timers;

import com.dod.UnrealZaruba.Utils.TimerCompletedCallback;
import com.dod.UnrealZaruba.Utils.TimerUpdatedCallback;

/**
 * A timer implementation based on game ticks
 */
public class TickTimer implements ITimer {
    private final int durationTicks; // Duration in game ticks
    private boolean running;
    private boolean completed;
    private boolean paused;
    private int currentAccumulatedTicks;
    
    private final TimerCompletedCallback completedCallback;
    private final TimerUpdatedCallback updatedCallback;
    
    /**
     * Creates a new tick-based timer
     * 
     * @param durationTicks Duration in game ticks
     * @param completedCallback Callback when timer completes
     * @param updatedCallback Callback when timer updates
     * @param autoStart Whether to start the timer immediately
     */
    public TickTimer(int durationTicks, TimerCompletedCallback completedCallback, 
                     TimerUpdatedCallback updatedCallback, boolean autoStart) {
        this.durationTicks = durationTicks;
        this.completedCallback = completedCallback;
        this.updatedCallback = updatedCallback;
        this.currentAccumulatedTicks = 0;
        this.completed = false;
        this.paused = false;
        
        if (autoStart) {
            start();
        }
    }
    
    @Override
    public void start() {
        if (!running && !completed) {
            running = true;
            paused = false;
        }
    }
    
    @Override
    public void update() {
        if (running && !paused && !completed) {
            currentAccumulatedTicks++;
            
            if (updatedCallback != null) {
                updatedCallback.run(currentAccumulatedTicks);
            }
            
            if (currentAccumulatedTicks >= durationTicks) {
                completed = true;
                running = false;
                
                if (completedCallback != null) {
                    completedCallback.run();
                }
                dispose(false);
            }
        }
    }
    
    @Override
    public void pause() {
        if (running && !paused) {
            paused = true;
        }
    }
    
    @Override
    public void resume() {
        if (running && paused) {
            paused = false;
        }
    }
    
    @Override
    public void reset() {
        running = false;
        completed = false;
        paused = false;
        currentAccumulatedTicks = 0;
    }
    
    @Override
    public void dispose(boolean complete) {
        running = false;
        
        if (complete && !completed && completedCallback != null) {
            completedCallback.run();
        }
        
        TimerManager.disposeTimer(this);
    }

    @Override
    public long getDuration() {
        return durationTicks;
    }

    @Override
    public float getProgress() {
        if (!running) {
            return completed ? 1.0f : 0.0f;
        }
        
        return Math.min(1.0f, (float)currentAccumulatedTicks / durationTicks);
    }
    
    @Override
    public long getRemainingTime() {
        if (!running || completed) {
            return 0;
        }
        
        int remainingTicks = durationTicks - currentAccumulatedTicks;
        return remainingTicks * 50; // Convert ticks to milliseconds (50ms per tick)
    }
    
    @Override
    public long getElapsedTime() {
        return currentAccumulatedTicks * 50; // Convert ticks to milliseconds
    }
    
    @Override
    public boolean isRunning() {
        return running && !paused;
    }
    
    @Override
    public boolean isCompleted() {
        return completed;
    }
} 