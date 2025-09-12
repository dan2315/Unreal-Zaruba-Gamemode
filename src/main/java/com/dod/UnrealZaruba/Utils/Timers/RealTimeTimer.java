package com.dod.UnrealZaruba.Utils.Timers;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.TimerCompletedCallback;
import com.dod.UnrealZaruba.Utils.TimerUpdatedCallback;

/**
 * A timer implementation based on real-world time
 */
public class RealTimeTimer implements ITimer {
    private final long duration;
    private long startTime;
    private long pausedAt;
    private long totalPausedTime;
    private boolean running;
    private boolean completed;
    private boolean paused;
    
    private final TimerCompletedCallback completedCallback;
    private final TimerUpdatedCallback updatedCallback;

    public RealTimeTimer(long durationMs, TimerCompletedCallback completedCallback, 
                         TimerUpdatedCallback updatedCallback, boolean autoStart) {
        this.duration = durationMs;
        this.completedCallback = completedCallback;
        this.updatedCallback = updatedCallback;
        this.totalPausedTime = 0;
        this.completed = false;
        this.paused = false;
        
        if (autoStart) {
            start();
        }
    }

    public RealTimeTimer(int durationSeconds, long startTime, TimerUpdatedCallback updatedCallback) {
        this.duration = durationSeconds * 1000L;
        this.completedCallback = null;
        this.updatedCallback = updatedCallback;
        start(startTime);
    }
    
    @Override
    public void start() {
        if (!running && !completed) {
            startTime = System.currentTimeMillis();
            running = true;
            paused = false;
        }
    }

    private void start(long startTime) {
        if (!running && !completed) {
            UnrealZaruba.LOGGER.warn("Starting RealTime timer");
            this.startTime = startTime;
            running = true;
            paused = false;
        }
    }
    
    @Override
    public void update() {
        if (running && !paused && !completed) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime - totalPausedTime;
            
            int elapsedTicks = (int)(elapsedTime / 50);

            if (updatedCallback != null) {
                updatedCallback.run(elapsedTicks);
            }
            
            if (elapsedTime >= duration) {
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
            pausedAt = System.currentTimeMillis();
        }
    }
    
    @Override
    public void resume() {
        if (running && paused) {
            totalPausedTime += System.currentTimeMillis() - pausedAt;
            paused = false;
        }
    }
    
    @Override
    public void reset() {
        running = false;
        completed = false;
        paused = false;
        totalPausedTime = 0;
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
        return duration;
    }

    @Override
    public float getProgress() {
        if (!running) {
            return completed ? 1.0f : 0.0f;
        }
        
        long elapsedTime = getElapsedTime();
        return Math.min(1.0f, (float)elapsedTime / duration);
    }
    
    @Override
    public long getRemainingTime() {
        if (!running || completed) {
            return 0;
        }
        
        long elapsedTime = getElapsedTime();
        return Math.max(0, duration - elapsedTime);
    }
    
    @Override
    public long getElapsedTime() {
        if (!running) {
            return completed ? duration : 0;
        }
        
        long currentTime = System.currentTimeMillis();
        if (paused) {
            return pausedAt - startTime - totalPausedTime;
        } else {
            return currentTime - startTime - totalPausedTime;
        }
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