package com.dod.UnrealZaruba.Gamemodes.Objectives;

public abstract class GameObjective {
    String name;
    public float progress;
    protected IProgressDisplay progressDisplay;
    
    public String GetName() {
        return name;
    }

    public IProgressDisplay getProgressDisplay() {
        return progressDisplay;
    }
    
    public void setProgressDisplay(IProgressDisplay display) {
        this.progressDisplay = display;
    }
    
    protected void updateProgressDisplay() {
        if (progressDisplay != null) {
            progressDisplay.updateProgress(progress);
        }
    }
    
    public abstract Boolean IsCompleted();

    public abstract void Update();
} 
