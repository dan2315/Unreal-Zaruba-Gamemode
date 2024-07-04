package com.dod.UnrealZaruba.Utils;

import java.util.ArrayList;


public class TimerManager {
    static ArrayList<TickTimer> activeTimers = new ArrayList<TickTimer>();

    public static TickTimer Create(int duration, TimerCompletedCallback completedCallback,
            TimerUpdatedCallback updatedCallback) {
        TickTimer timer = new TickTimer(duration, false) {
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

    public static void UpdateAll() {
        for (int i = 0; i < activeTimers.size(); i++) {
            activeTimers.get(i).Update();
        }

    }

    public static void DisposeTimer(TickTimer timer) {
        activeTimers.remove(timer);
        timer = null;
    }
}
