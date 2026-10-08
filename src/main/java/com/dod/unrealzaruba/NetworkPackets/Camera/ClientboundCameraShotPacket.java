package com.dod.unrealzaruba.NetworkPackets.Camera;

import java.util.function.Supplier;

import com.dod.unrealzaruba.Camera.ClientCamera;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;

/** Tells a camera client to take a screenshot once the view at the given position has rendered. */
public class ClientboundCameraShotPacket implements ModPacket {
    public final double x, y, z;
    public final float yaw, pitch, fov;
    public final String name;

    public ClientboundCameraShotPacket(double x, double y, double z, float yaw, float pitch, float fov, String name) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.fov = fov;
        this.name = name;
    }

    public static void encode(ClientboundCameraShotPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
        buf.writeFloat(msg.fov);
        buf.writeUtf(msg.name, 64);
    }

    public static ClientboundCameraShotPacket decode(FriendlyByteBuf buf) {
        return new ClientboundCameraShotPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readUtf(64));
    }

    public static void handle(ClientboundCameraShotPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> ClientCamera.start(msg));
        ctx.get().setPacketHandled(true);
    }
}
