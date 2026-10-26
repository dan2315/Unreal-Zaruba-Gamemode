package com.dod.unrealzaruba.NetworkPackets.MapEditor;

import java.util.function.Supplier;

import com.dod.unrealzaruba.MapEditor.EditorState;
import com.dod.unrealzaruba.MapEditor.MapEditorClient;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;

/** The open map's objectives and bases for an op's map editor, sent on request and after every change. */
public class ClientboundMapEditorStatePacket implements ModPacket {
    private final EditorState state;

    public ClientboundMapEditorStatePacket(EditorState state) {
        this.state = state;
    }

    public static void encode(ClientboundMapEditorStatePacket msg, FriendlyByteBuf buf) {
        msg.state.encode(buf);
    }

    public static ClientboundMapEditorStatePacket decode(FriendlyByteBuf buf) {
        return new ClientboundMapEditorStatePacket(EditorState.decode(buf));
    }

    public static void handle(ClientboundMapEditorStatePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> MapEditorClient.setState(msg.state));
        ctx.get().setPacketHandled(true);
    }
}
