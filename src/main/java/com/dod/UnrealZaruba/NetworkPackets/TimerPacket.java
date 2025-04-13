package com.dod.UnrealZaruba.NetworkPackets;

import java.util.function.Supplier;

import com.dod.UnrealZaruba.UI.TimerOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class TimerPacket {
    
    private int seconds;
    private int minutes;
    private boolean isVisible;

    public TimerPacket(int seconds, int minutes, boolean isVisible) {
        this.seconds = seconds;
        this.minutes = minutes;
        this.isVisible = isVisible;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getMinutes() {   
        return minutes;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public static void encode(TimerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.getSeconds());
        buffer.writeInt(packet.getMinutes());
        buffer.writeBoolean(packet.isVisible());
    }

    public static TimerPacket decode(FriendlyByteBuf buffer) {
        int seconds = buffer.readInt();
        int minutes = buffer.readInt();
        boolean isVisible = buffer.readBoolean();
        return new TimerPacket(seconds, minutes, isVisible);
    }

    // Handle the packet on the client side
    public static void handle(TimerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            TimerOverlay.OVERLAY_INSTANCE.update(packet.seconds, packet.minutes, packet.isVisible);
        });
        ctx.get().setPacketHandled(true);
    }
}
