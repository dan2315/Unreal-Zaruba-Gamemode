package com.dod.UnrealZaruba.Gamemodes.RespawnPoints;

public abstract class BaseRespawnPoint implements IRespawnPoint {
    private final byte runtimeId;
    static byte NEXT_ID = 0;

    public BaseRespawnPoint() {
        runtimeId = NEXT_ID++;
    }

    @Override
    public byte getRuntimeId() {
        return runtimeId;
    }
}
