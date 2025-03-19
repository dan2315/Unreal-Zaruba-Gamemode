package com.dod.UnrealZaruba.Utils.Timers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.dod.UnrealZaruba.Utils.TimerCompletedCallback;
import com.dod.UnrealZaruba.Utils.TimerUpdatedCallback;

/**
 * Manages all timers in the game
 */
public class TimerManager {
    private static final List<ITimer> activeTimers = new CopyOnWriteArrayList<>();
    
    /**
     * Creates a new tick-based timer
     * 
     * @param durationTicks Duration in game ticks
     * @param completedCallback Callback when timer completes
     * @param updatedCallback Callback when timer updates
     * @param preventStart Whether to prevent auto-start
     * @return The created timer
     */
    public static TickTimer createTickTimer(int durationTicks, TimerCompletedCallback completedCallback,
                                           TimerUpdatedCallback updatedCallback, boolean preventStart) {
        TickTimer timer = new TickTimer(durationTicks, completedCallback, updatedCallback, !preventStart);
        activeTimers.add(timer);
        return timer;
    }
    
    /**
     * Creates a new tick-based timer that auto-starts
     * 
     * @param durationTicks Duration in game ticks
     * @param completedCallback Callback when timer completes
     * @param updatedCallback Callback when timer updates
     * @return The created timer
     */
    public static TickTimer createTickTimer(int durationTicks, TimerCompletedCallback completedCallback,
                                           TimerUpdatedCallback updatedCallback) {
        return createTickTimer(durationTicks, completedCallback, updatedCallback, false);
    }
    
    /**
     * Creates a new real-time timer
     * 
     * @param durationMs Duration in milliseconds
     * @param completedCallback Callback when timer completes
     * @param updatedCallback Callback when timer updates
     * @param preventStart Whether to prevent auto-start
     * @return The created timer
     */
    public static RealTimeTimer createRealTimeTimer(long durationMs, TimerCompletedCallback completedCallback,
                                                  TimerUpdatedCallback updatedCallback, boolean preventStart) {
        RealTimeTimer timer = new RealTimeTimer(durationMs, completedCallback, updatedCallback, !preventStart);
        activeTimers.add(timer);
        return timer;
    }
    
    /**
     * Creates a new real-time timer that auto-starts
     * 
     * @param durationMs Duration in milliseconds
     * @param completedCallback Callback when timer completes
     * @param updatedCallback Callback when timer updates
     * @return The created timer
     */
    public static RealTimeTimer createRealTimeTimer(long durationMs, TimerCompletedCallback completedCallback,
                                                  TimerUpdatedCallback updatedCallback) {
        return createRealTimeTimer(durationMs, completedCallback, updatedCallback, false);
    }
    
    /**
     * Updates all active timers
     */
    public static void updateAll() {
        for (ITimer timer : activeTimers) {
            timer.update();
        }
    }
    
    /**
     * Disposes a timer
     * 
     * @param timer The timer to dispose
     */
    public static void disposeTimer(ITimer timer) {
        activeTimers.remove(timer);
    }
    
    /**
     * Disposes a timer with option to complete
     * 
     * @param timer The timer to dispose
     * @param complete Whether to trigger completion callback
     */
    public static void disposeTimer(ITimer timer, boolean complete) {
        if (complete) {
            timer.dispose(true);
        } else {
            disposeTimer(timer);
        }
    }
    
    /**
     * Pauses all active timers
     */
    public static void pauseAll() {
        for (ITimer timer : activeTimers) {
            timer.pause();
        }
    }
    
    /**
     * Resumes all active timers
     */
    public static void resumeAll() {
        for (ITimer timer : activeTimers) {
            timer.resume();
        }
    }
    
    /**
     * Gets the count of active timers
     * 
     * @return Number of active timers
     */
    public static int getActiveTimerCount() {
        return activeTimers.size();
    }
}
