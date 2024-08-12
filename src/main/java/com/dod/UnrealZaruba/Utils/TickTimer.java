package com.dod.UnrealZaruba.Utils;

import com.dod.UnrealZaruba.UnrealZaruba;

public class TickTimer {

    private int duration; // time in milliseconds
    private boolean started;
    private int currentAccumulatedTicks;

    public TickTimer(int duration, boolean preventStart)
    {
        this.duration = duration;

        if (!preventStart) Start();
    }

    public void Start() 
    {
        started = true;
    }

    public void Update() 
    {
        if (started)
        {
            currentAccumulatedTicks++;

            OnUpdated(currentAccumulatedTicks);
            if (currentAccumulatedTicks * 50 >= duration) {
                OnCompleted();
                Dispose();
            }
        }
    }

    public void OnUpdated(int currentAccumulatedTicks) {}

    public void OnCompleted() {}

    private void Dispose() {
        TimerManager.DisposeTimer(this, false);
    }

    public void Dispose(boolean complete) {
        TimerManager.DisposeTimer(this, complete);
    }
}
