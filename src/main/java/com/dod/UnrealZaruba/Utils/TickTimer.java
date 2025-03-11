package com.dod.UnrealZaruba.Utils;

public class TickTimer {

    private int duration; // time in milliseconds
    private boolean started;
    private int currentAccumulatedTicks;

    /**
     * Instantiates a new Tick timer.
     *
     * @param duration     the duration
     * @param preventStart the prevent start
     */
    public TickTimer(int duration, boolean preventStart)
    {
        this.duration = duration;

        if (!preventStart) Start();
    }

    /**
     * Start.
     */
    public void Start()
    {
        started = true;
    }

    /**
     * Update.
     */
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

    /**
     * On updated.
     *
     * @param currentAccumulatedTicks the current accumulated ticks
     */
    public void OnUpdated(int currentAccumulatedTicks) {}

    /**
     * On completed.
     */
    public void OnCompleted() {}

    private void Dispose() {
        TimerManager.DisposeTimer(this, false);
    }

    /**
     * Dispose.
     *
     * @param complete the complete
     */
    public void Dispose(boolean complete) {
        TimerManager.DisposeTimer(this, complete);
    }
}
