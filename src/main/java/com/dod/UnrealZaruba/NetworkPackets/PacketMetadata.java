package com.dod.UnrealZaruba.NetworkPackets;

import java.util.UUID;

public class PacketMetadata {
    private final long sendTime;
    private final UUID playerUUID;
    private final boolean fromServer;
    private final Object packet;

    public PacketMetadata(long sendTime, UUID playerUUID, boolean fromServer, Object packet) {
        this.sendTime = sendTime;
        this.playerUUID = playerUUID;
        this.fromServer = fromServer;
        this.packet = packet;
    }

    public long getSendTime() {
        return sendTime;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public boolean isFromServer() {
        return fromServer;
    }

    public Object getPacket() {
        return packet;
    }
}
