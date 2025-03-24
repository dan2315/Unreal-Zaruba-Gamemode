package com.dod.UnrealZaruba.Gamemodes.GamePhases;

import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import java.util.function.Consumer;

public class TimedGamePhase extends AbstractGamePhase {
    public int duration; // milliseconds
    public Runnable onStart;
    public Runnable onCompleted;
    public Consumer<Integer> onTick;

    public TimedGamePhase(PhaseId phaseId, int duration, Runnable onStart, Consumer<Integer> onTick, Runnable onCompleted) {
        super(phaseId);
        this.duration = duration;
        this.onStart = onStart;
        this.onTick = onTick;
        this.onCompleted = onCompleted;
    }

    @Override
    public void OnStart() {
        onStart.run();
        TimerManager.createRealTimeTimer(duration, this::OnCompleted, this::OnTick); 
    }

    @Override
    public void OnCompleted() {
        onCompleted.run();
    }

    @Override
    public void OnTick(int ticks) {
        onTick.accept(ticks);
    }
    
}
