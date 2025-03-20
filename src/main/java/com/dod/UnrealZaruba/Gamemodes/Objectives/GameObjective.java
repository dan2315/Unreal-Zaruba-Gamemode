package com.dod.UnrealZaruba.Gamemodes.Objectives;

public abstract class GameObjective {
    public float progress;
    protected IProgressDisplay progressDisplay;
    
    /**
     * Gets the progress display for this objective
     * @return The progress display, or null if not set
     */
    public IProgressDisplay getProgressDisplay() {
        return progressDisplay;
    }
    
    /**
     * Sets the progress display for this objective
     * @param display The progress display to use
     */
    public void setProgressDisplay(IProgressDisplay display) {
        this.progressDisplay = display;
    }
    
    /**
     * Updates the progress display if one is set
     */
    protected void updateProgressDisplay() {
        if (progressDisplay != null) {
            progressDisplay.updateProgress(progress);
        }
    }
    
    public abstract Boolean IsCompleted();

    public abstract void Update();
} 
