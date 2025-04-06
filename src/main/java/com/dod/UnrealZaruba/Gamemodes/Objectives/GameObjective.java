package com.dod.UnrealZaruba.Gamemodes.Objectives;


public abstract class GameObjective {
    String name;
    public float progress;
    private boolean isCompleted = false;
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
    
    public Boolean IsCompleted() {
        return isCompleted;
    }

    public void Complete() {
        isCompleted = true;
        OnCompleted();
    }

    public void Reset() {
        isCompleted = false;
    }

    protected abstract void OnCompleted();

    protected abstract boolean UpdateImplementation();

    public void Update() {
        if (isCompleted) return;
        if (UpdateImplementation()) {
            Complete();
        }
    }
} 
