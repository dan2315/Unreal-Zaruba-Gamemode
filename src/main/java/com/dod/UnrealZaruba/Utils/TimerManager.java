package com.dod.UnrealZaruba.Utils;
import java.util.ArrayList;


public class TimerManager {
    static ArrayList<TickTimer> activeTimers = new ArrayList<TickTimer>();

    public static TickTimer Create(int duration, TimerCompletedCallback completedCallback, TimerUpdatedCallback updatedCallback)
    {
        TickTimer timer = new TickTimer(duration, false)
        {
            @Override
            public void OnCompleted() {
                completedCallback.run();
            }

            @Override
            public void OnUpdated(int ticks) {
                updatedCallback.run(ticks);
            }
        };

        return timer;
    }

    public static void UpdateAll()
    {
        for (TickTimer timer : activeTimers) {
            timer.Update();
        }
    }
    
    public static void DisposeTimer(TickTimer timer) {
        activeTimers.remove(timer);
        timer = null;
    }
}
