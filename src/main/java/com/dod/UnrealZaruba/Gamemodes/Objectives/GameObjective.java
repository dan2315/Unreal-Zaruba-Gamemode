package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.util.ArrayList;
import java.util.List;

public abstract class GameObjective {
    String name;
    String type; // Used to determine objective type during deserialization
    public float progress;
    private boolean isCompleted = false;
    protected IProgressDisplay progressDisplay;
    
    private final transient List<ObjectiveOwner> notificationRecipients = new ArrayList<>();

    public GameObjective() {
        // Default constructor needed for deserialization
    }
    
    public GameObjective(String name, String type) {
        this.name = name;
        this.type = type;
        this.progress = 0.0f;
    }
    
    public String GetName() {
        return name;
    }
    
    public String getType() {
        return type;
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
    
    public void addNotificationRecipient(ObjectiveOwner recipient) {
        if (recipient != null && !notificationRecipients.contains(recipient)) {
            notificationRecipients.add(recipient);
        }
    }
    
    public void removeNotificationRecipient(ObjectiveOwner recipient) {
        notificationRecipients.remove(recipient);
    }

    protected void sendObjectiveChanged(GameObjective objective) {
        for (ObjectiveOwner recipient : notificationRecipients) {
            recipient.onObjectiveStateChanged(objective);
        }
    }

    protected void sendObjectiveCompleted(GameObjective objective) {
        for (ObjectiveOwner recipient : notificationRecipients) {
            recipient.onObjectiveCompleted(objective);
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
