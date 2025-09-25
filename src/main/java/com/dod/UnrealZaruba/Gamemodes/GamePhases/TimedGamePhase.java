package com.dod.unrealzaruba.Gamemodes.GamePhases;

import com.dod.unrealzaruba.utils.Timers.ITimer;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import java.util.function.Consumer;

public class TimedGamePhase extends AbstractGamePhase {
    public int duration; // milliseconds
    public Runnable onStart;
    public Runnable onCompleted;
    public Consumer<Integer> onTick;
    public ITimer timer;
    public IPhaseHolder phaseHolder;

    @Deprecated //("Use ConditionalPhase with TimePassedCondition instead")
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
        timer = TimerManager.createRealTimeTimer(duration, this::OnCompleted, this::OnTick);
    }

    @Override
    public void OnCompleted() {
        Clear();
        onCompleted.run();
    }

    @Override
    public void OnTick(int ticks) {
        onTick.accept(ticks);
    }

    @Override
    public void Clear() {
        if (timer != null) {
            timer.dispose(false);
        }
        if (phaseHolder != null) { // TODO: Assign phase holder when AddPhase is called
            phaseHolder.CompletePhase(this.phaseId);
        }
    }
}
