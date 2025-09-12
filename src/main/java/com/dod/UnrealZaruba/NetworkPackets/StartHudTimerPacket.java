package com.dod.UnrealZaruba.NetworkPackets;

import java.util.function.Supplier;

import com.dod.UnrealZaruba.UI.TimerOverlay;
import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class StartHudTimerPacket {
    
    private long startTime;
    private int durationSeconds;

    public StartHudTimerPacket(long startTime, int durationSeconds) {
        this.startTime = startTime;
        this.durationSeconds = durationSeconds;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public static void encode(StartHudTimerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.startTime);
        buffer.writeInt(packet.durationSeconds);
    }

    public static StartHudTimerPacket decode(FriendlyByteBuf buffer) {
        long startTime = buffer.readLong();
        int durationSeconds = buffer.readInt();
        return new StartHudTimerPacket(startTime, durationSeconds);
    }


    public static void handle(StartHudTimerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        UnrealZaruba.LOGGER.warn("HANDLING START TIMER PACKET");
        TimerOverlay.OVERLAY_INSTANCE.startCountDown(packet.getStartTime(), packet.getDurationSeconds());
        ctx.get().setPacketHandled(true);
    }
}
