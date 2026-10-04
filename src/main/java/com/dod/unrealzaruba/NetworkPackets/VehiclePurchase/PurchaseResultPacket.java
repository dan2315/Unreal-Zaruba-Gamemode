package com.dod.unrealzaruba.NetworkPackets.VehiclePurchase;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import java.util.function.Supplier;

public class PurchaseResultPacket implements ModPacket {
    private final boolean success;
    private final String message;

    public PurchaseResultPacket(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static void encode(PurchaseResultPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.success);
        buf.writeUtf(msg.message != null ? msg.message : "");
    }

    public static PurchaseResultPacket decode(FriendlyByteBuf buf) {
        return new PurchaseResultPacket(buf.readBoolean(), buf.readUtf());
    }

    public static void handle(PurchaseResultPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            
                UnrealZaruba.LOGGER.info("[UnrealZaruba] Received purchase result: success={}, message={}",
                    msg.success, msg.message);
                
                if (Minecraft.getInstance().screen instanceof com.dod.unrealzaruba.UI.VehiclePurchaseMenu.VehiclePurchaseScreen screen) {
                    if (msg.success) {
                        Minecraft.getInstance().setScreen(null);
                    } else {
                        screen.setErrorMessage(msg.message);
                    }
                }
        });
        ctx.get().setPacketHandled(true);
    }
}