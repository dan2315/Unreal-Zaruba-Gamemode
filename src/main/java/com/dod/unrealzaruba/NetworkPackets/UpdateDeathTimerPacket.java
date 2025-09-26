package com.dod.unrealzaruba.NetworkPackets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.dod.unrealzaruba.UI.CustomDeathScreen;
import net.minecraft.client.Minecraft;

public class UpdateDeathTimerPacket {
    
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
    public static void handle(UpdateDeathTimerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Update the client-side death timer UI
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof CustomDeathScreen ) {
                ((CustomDeathScreen) minecraft.screen).updateRespawnTimer(packet.getRemainingTime());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
