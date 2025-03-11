package com.dod.UnrealZaruba.Utils;

import java.util.ArrayList;

public class TimerManager {
    /**
     * The Active timers.
     */
    static ArrayList<TickTimer> activeTimers = new ArrayList<TickTimer>();

    /**
     * Create tick timer.
     *
     * @param duration          the duration
     * @param completedCallback the completed callback
     * @param updatedCallback   the updated callback
     * @param preventStart      the prevent start
     * @return the tick timer
     */
    public static TickTimer Create(int duration, TimerCompletedCallback completedCallback,
            TimerUpdatedCallback updatedCallback, boolean preventStart) {
                TickTimer timer = new TickTimer(duration, preventStart) {
                    @Override
                    public void OnCompleted() {
                        completedCallback.run();
                    }
        
                    @Override
                    public void OnUpdated(int ticks) {
                        updatedCallback.run(ticks);
                    }
                };
        
                activeTimers.add(timer);
        
                return timer;
            }

    /**
     * Create tick timer.
     *
     * @param duration          the duration
     * @param completedCallback the completed callback
     * @param updatedCallback   the updated callback
     * @return the tick timer
     */
    public static TickTimer Create(int duration, TimerCompletedCallback completedCallback,
            TimerUpdatedCallback updatedCallback) {
                return Create(duration, completedCallback, updatedCallback, false);
    }

    /**
     * Update all.
     */
    public static void UpdateAll() {
        for (int i = 0; i < activeTimers.size(); i++) {
            activeTimers.get(i).Update();
        }
    }

    /**
     * Dispose timer.
     *
     * @param timer    the timer
     * @param complete the complete
     */
    public static void DisposeTimer(TickTimer timer, boolean complete) {
        activeTimers.remove(timer);
        if (complete) {
            timer.OnCompleted();
        }
    }
}
