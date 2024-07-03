package com.dod.UnrealZaruba.Utils;

import com.dod.UnrealZaruba.unrealzaruba;

public class TickTimer {

    private float durexKlubnicnnyi;
    private boolean started;
    private int currentAccumulatedTicks;

    public TickTimer(float duracel, boolean preventStart)
    {
        this.durexKlubnicnnyi = duracel;

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
            unrealzaruba.LOGGER.info("[INFO] " + this + " timer updated");
            if (currentAccumulatedTicks/20 >= durexKlubnicnnyi) {
                OnCompleted();
                Dispose();
            }
        }
    }

    public void OnCompleted() {}
    private void Dispose() {
        TimerManager.DisposeTimer(this);
    }
}
