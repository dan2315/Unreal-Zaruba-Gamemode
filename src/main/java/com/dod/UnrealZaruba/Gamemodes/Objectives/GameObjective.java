package com.dod.UnrealZaruba.Gamemodes.Objectives;

import com.dod.UnrealZaruba.Gamemodes.Objectives.ProgressDisplay.IProgressDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class GameObjective {
    String name;
    String type;
    private float progress; // should be from 0.0 to 1.0
    private boolean isCompleted = false;

    private transient float previousProgress;
    protected transient IProgressDisplay progressDisplay;
    private transient Consumer<GameObjective> onCompleted;
    private transient List<ObjectiveOwner> notificationRecipients = new ArrayList<>();
    private transient byte runtimeId;
    public static byte LastRuntimeId = 0;

    public GameObjective() {
        // Default constructor needed for deserialization
    }

    public void InitializeAfterSerialization() {
        runtimeId = LastRuntimeId++;
        notificationRecipients = new ArrayList<>();
    }

    public GameObjective(String name, String type) {
        this.name = name;
        this.type = type;
        this.progress = 0.0f;
    }
    
    public String GetName() {
        return name;
    }
    
    public String GetType() {
        return type;
    }
    
    public float GetProgress() {
        return progress;
    }
    
    public void SetProgress(float value) {
        progress = value;
        PostProcessProgressChange();
    }

    public void AddProgress(float value) {
        progress += value;
        PostProcessProgressChange();
    }

    private void PostProcessProgressChange() {
        if (progress < 0) {
            progress = 0;
        }
        if (progress != previousProgress) {
            updateProgressDisplay();
        }
        previousProgress = progress;
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
        this.onCompleted.accept(this);
    }

    protected void SetCompleted(boolean value) {
        isCompleted = value;
    }

    public void Reset() {
        isCompleted = false;
    }

    public void SubscribeOnCompleted(Consumer<GameObjective> onCompleted) {
        this.onCompleted = onCompleted;
    }

    protected abstract void OnCompleted();

    protected abstract boolean UpdateImplementation();  // returning true means that objective is completed

    public void Update() {
        if (isCompleted) return;
        if (UpdateImplementation()) {
            Complete();
        }
    }

    public Byte GetRuntimeId() {
        return runtimeId;
    }
}
