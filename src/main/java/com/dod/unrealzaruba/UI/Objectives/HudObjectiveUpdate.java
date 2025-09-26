package com.dod.unrealzaruba.UI.Objectives;

public class HudObjectiveUpdate {
    private final byte runtimeId;
    private final byte progress;

    public HudObjectiveUpdate(byte runtimeId, byte progress) {
        this.runtimeId = runtimeId;
        this.progress = progress;
    }

    public byte getRuntimeId() {
        return runtimeId;
    }

    public byte getProgress() {
        return progress;
    }
}
