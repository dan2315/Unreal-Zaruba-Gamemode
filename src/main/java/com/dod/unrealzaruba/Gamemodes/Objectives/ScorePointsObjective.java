package com.dod.unrealzaruba.Gamemodes.Objectives;


import com.dod.unrealzaruba.UnrealZaruba;

public class ScorePointsObjective extends GameObjective {
    final int requiredPoints;
    float incrementationSpeed;
    float currentPoints;
    private int updateCounter;

    public ScorePointsObjective(Integer incrementationSpeed, int requiredPoints) {
        this.incrementationSpeed = incrementationSpeed;
        this.requiredPoints = requiredPoints;
    }

    public void SetIncrementationSpeed(float incrementationSpeed) {
        this.incrementationSpeed = incrementationSpeed;
        UnrealZaruba.LOGGER.info("Increment speed was set to {}", incrementationSpeed);
    }

    @Override
    protected void OnCompleted() {

    }

    @Override
    protected boolean UpdateImplementation() {
        updateCounter++;
        if (updateCounter % 10 != 0) return false; // update every half second

        currentPoints += incrementationSpeed;
        SetProgress(currentPoints /requiredPoints);
        return currentPoints >= requiredPoints;
    }
}
