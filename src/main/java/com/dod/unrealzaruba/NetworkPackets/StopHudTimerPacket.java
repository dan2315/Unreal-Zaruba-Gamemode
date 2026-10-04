package com.dod.unrealzaruba.NetworkPackets;

import com.dod.unrealzaruba.UI.TimerOverlay;
import net.minecraft.network.FriendlyByteBuf;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import java.util.function.Supplier;

public class StopHudTimerPacket implements ModPacket {

    public static void encode(StopHudTimerPacket packet, FriendlyByteBuf buffer) {

    }

    public static StopHudTimerPacket decode(FriendlyByteBuf buffer) {

        return new StopHudTimerPacket();
    }


    public static void handle(StopHudTimerPacket packet, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            TimerOverlay.OVERLAY_INSTANCE.stop();
        });
        ctx.get().setPacketHandled(true);
    }
}
