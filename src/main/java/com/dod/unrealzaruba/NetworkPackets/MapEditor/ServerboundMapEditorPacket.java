package com.dod.unrealzaruba.NetworkPackets.MapEditor;

import java.util.function.Supplier;

import com.dod.unrealzaruba.MapEditor.MapEditorService;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * An op's map editor asks for the open map's state or changes it. {@code index} is the objective's place in the map's
 * objectives list (-1 for a new one); {@code a}/{@code b} are the corners of an area, or {@code a} is a position.
 */
public class ServerboundMapEditorPacket implements ModPacket {
    public enum Action {
        REQUEST,
        SAVE_POINT,
        DELETE_POINT,
        SET_POINT_RESPAWN,
        SAVE_TARGET,
        DELETE_TARGET,
        SET_BASE
    }

    public final Action action;
    public final int index;
    public final String name;
    /** Initial owner of a capture point (RED, BLUE or "" for neutral), or the team of a base. */
    public final String owner;
    public final BlockPos a;
    public final BlockPos b;
    public final float required;

    public ServerboundMapEditorPacket(Action action, int index, String name, String owner, BlockPos a, BlockPos b, float required) {
        this.action = action;
        this.index = index;
        this.name = name;
        this.owner = owner;
        this.a = a;
        this.b = b;
        this.required = required;
    }

    public static ServerboundMapEditorPacket request() {
        return new ServerboundMapEditorPacket(Action.REQUEST, -1, "", "", BlockPos.ZERO, BlockPos.ZERO, 0);
    }

    public static void encode(ServerboundMapEditorPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeVarInt(msg.index);
        buf.writeUtf(msg.name, 64);
        buf.writeUtf(msg.owner, 8);
        buf.writeBlockPos(msg.a);
        buf.writeBlockPos(msg.b);
        buf.writeFloat(msg.required);
    }

    public static ServerboundMapEditorPacket decode(FriendlyByteBuf buf) {
        return new ServerboundMapEditorPacket(buf.readEnum(Action.class), buf.readVarInt(), buf.readUtf(64), buf.readUtf(8),
                buf.readBlockPos(), buf.readBlockPos(), buf.readFloat());
    }

    public static void handle(ServerboundMapEditorPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MapEditorService.handle(player, msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
