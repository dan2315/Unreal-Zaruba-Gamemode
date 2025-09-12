package com.dod.UnrealZaruba.Utils.Timers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.TimerCompletedCallback;
import com.dod.UnrealZaruba.Utils.TimerUpdatedCallback;

/**
 * Manages all timers in the game
 */
public class TimerManager {
    private static final List<ITimer> activeTimers = new CopyOnWriteArrayList<>();

    public static TickTimer createTickTimer(int durationTicks, TimerCompletedCallback completedCallback,
                                           TimerUpdatedCallback updatedCallback, boolean preventStart) {
        TickTimer timer = new TickTimer(durationTicks, completedCallback, updatedCallback, !preventStart);
        activeTimers.add(timer);
        return timer;
    }

    public static TickTimer createTickTimer(int durationTicks, TimerCompletedCallback completedCallback,
                                           TimerUpdatedCallback updatedCallback) {
        return createTickTimer(durationTicks, completedCallback, updatedCallback, false);
    }

    public static RealTimeTimer createRealTimeTimer(long durationMs, TimerCompletedCallback completedCallback,
                                                  TimerUpdatedCallback updatedCallback, boolean preventStart) {
        RealTimeTimer timer = new RealTimeTimer(durationMs, completedCallback, updatedCallback, !preventStart);
        activeTimers.add(timer);
        return timer;
    }

    public static RealTimeTimer createRealTimeTimer(long durationMs, TimerCompletedCallback completedCallback,
                                                  TimerUpdatedCallback updatedCallback) {
        return createRealTimeTimer(durationMs, completedCallback, updatedCallback, false);
    }

    public static RealTimeTimer createRealTimeTimer(int durationSeconds, long startTime,TimerUpdatedCallback updatedCallback)
    {
        RealTimeTimer timer = new RealTimeTimer(durationSeconds, startTime, updatedCallback);
        activeTimers.add(timer);
        return timer;
    }

    public static void updateAll() {
        for (ITimer timer : activeTimers) {
            timer.update();
        }
    }

    public static void disposeTimer(ITimer timer) {
        activeTimers.remove(timer);
    }

    public static void disposeTimer(ITimer timer, boolean complete) {
        if (complete) {
            timer.dispose(true);
        } else {
            disposeTimer(timer);
        }
    }

    public static void pauseAll() {
        for (ITimer timer : activeTimers) {
            timer.pause();
        }
    }

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
