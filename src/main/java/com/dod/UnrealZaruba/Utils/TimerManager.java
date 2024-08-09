package com.dod.UnrealZaruba.Utils;

import java.util.ArrayList;

public class TimerManager {
    static ArrayList<TickTimer> activeTimers = new ArrayList<TickTimer>();

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

    public static TickTimer Create(int duration, TimerCompletedCallback completedCallback,
            TimerUpdatedCallback updatedCallback) {
                return Create(duration, completedCallback, updatedCallback, false);
    }

    public static void UpdateAll() {
        for (int i = 0; i < activeTimers.size(); i++) {
            activeTimers.get(i).Update();
        }
    }

    public static void DisposeTimer(TickTimer timer, boolean complete) {
        activeTimers.remove(timer);
        if (complete) {
            timer.OnCompleted();
        }
    }
}
