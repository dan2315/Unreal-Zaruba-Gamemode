package com.dod.unrealzaruba.NetworkPackets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.Respawn.ClientRespawnHandlers;

public class UpdateDeathTimerPacket implements ModPacket {
    
    private int remainingTime;

    public UpdateDeathTimerPacket(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public static void encode(UpdateDeathTimerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.getRemainingTime());
    }

    // Decode the packet data from the buffer
    public static UpdateDeathTimerPacket decode(FriendlyByteBuf buffer) {
        int remainingTime = buffer.readInt();
        return new UpdateDeathTimerPacket(remainingTime);
    }

    // Handle the packet on the client side
    public static void handle(UpdateDeathTimerPacket packet, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientRespawnHandlers.onTimer(packet.getRemainingTime());
        });
        ctx.get().setPacketHandled(true);
    }
}
