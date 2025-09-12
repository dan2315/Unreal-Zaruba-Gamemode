package com.dod.UnrealZaruba.NetworkPackets;

import com.dod.UnrealZaruba.UI.TimerOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StopHudTimerPacket {

    public static void encode(StopHudTimerPacket packet, FriendlyByteBuf buffer) {

    }

    public static StopHudTimerPacket decode(FriendlyByteBuf buffer) {

        return new StopHudTimerPacket();
    }


    public static void handle(StopHudTimerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            TimerOverlay.OVERLAY_INSTANCE.stop();
        });
        ctx.get().setPacketHandled(true);
    }
}
