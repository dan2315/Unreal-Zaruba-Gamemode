package com.dod.unrealzaruba.NetworkPackets.Radio;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Radiopings.PingService;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * A ping placed by crosshair. The client sends what it aimed at (block, entity or sky) plus the eye ray,
 * so the server can snap the ping onto a vehicle hull the ray actually hits instead of the ground behind it.
 */
public class ServerboundPingPacket implements ModPacket {
    private final byte typeId;
    private final BlockPos pos;
    private final int entityId;
    private final Vec3 eye;
    private final Vec3 look;
    private final boolean attach;

    public ServerboundPingPacket(byte typeId, BlockPos pos, int entityId, Vec3 eye, Vec3 look, boolean attach) {
        this.typeId = typeId;
        this.pos = pos;
        this.entityId = entityId;
        this.eye = eye;
        this.look = look;
        this.attach = attach;
    }

    public static void encode(ServerboundPingPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.typeId);
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.entityId);
        buf.writeDouble(msg.eye.x);
        buf.writeDouble(msg.eye.y);
        buf.writeDouble(msg.eye.z);
        buf.writeDouble(msg.look.x);
        buf.writeDouble(msg.look.y);
        buf.writeDouble(msg.look.z);
        buf.writeBoolean(msg.attach);
    }

    public static ServerboundPingPacket decode(FriendlyByteBuf buf) {
        byte typeId = buf.readByte();
        BlockPos pos = buf.readBlockPos();
        int entityId = buf.readInt();
        Vec3 eye = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 look = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        boolean attach = buf.readBoolean();
        return new ServerboundPingPacket(typeId, pos, entityId, eye, look, attach);
    }

    public static void handle(ServerboundPingPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PingService.onPing(player, msg.typeId, msg.pos, msg.entityId, msg.eye, msg.look, msg.attach);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
