package com.dod.unrealzaruba.NetworkPackets.Camera;

import java.util.function.Supplier;

import com.dod.unrealzaruba.Camera.CameraShots;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;

/** Camera client's report: where the screenshot went, or why it failed. */
public class CameraShotResultPacket implements ModPacket {
    public final boolean success;
    public final String name;
    public final String message;

    public CameraShotResultPacket(boolean success, String name, String message) {
        this.success = success;
        this.name = name;
        this.message = message;
    }

    public static void encode(CameraShotResultPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.success);
        buf.writeUtf(msg.name, 64);
        buf.writeUtf(msg.message, 512);
    }

    public static CameraShotResultPacket decode(FriendlyByteBuf buf) {
        return new CameraShotResultPacket(buf.readBoolean(), buf.readUtf(64), buf.readUtf(512));
    }

    public static void handle(CameraShotResultPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            var sender = ctx.get().getSender();
            if (sender != null) {
                CameraShots.onResult(sender, msg.success, msg.name, msg.message);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
