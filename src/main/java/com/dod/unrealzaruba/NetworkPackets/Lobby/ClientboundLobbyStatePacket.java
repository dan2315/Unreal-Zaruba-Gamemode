package com.dod.unrealzaruba.NetworkPackets.Lobby;

import java.util.Map;
import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.Lobby.LobbyClientState;

import net.minecraft.network.FriendlyByteBuf;

/** What the lobby screens show: whether voting is on, readiness counts, the own state of the player and the votes. */
public class ClientboundLobbyStatePacket implements ModPacket {
    public final boolean active;
    public final int ready;
    public final int players;
    public final int minPlayers;
    public final boolean myReady;
    public final String myVote;
    public final Map<String, Integer> votes;

    public ClientboundLobbyStatePacket(boolean active, int ready, int players, int minPlayers, boolean myReady,
                                       String myVote, Map<String, Integer> votes) {
        this.active = active;
        this.ready = ready;
        this.players = players;
        this.minPlayers = minPlayers;
        this.myReady = myReady;
        this.myVote = myVote;
        this.votes = votes;
    }

    public static void encode(ClientboundLobbyStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeVarInt(msg.ready);
        buf.writeVarInt(msg.players);
        buf.writeVarInt(msg.minPlayers);
        buf.writeBoolean(msg.myReady);
        buf.writeUtf(msg.myVote);
        buf.writeMap(msg.votes, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
    }

    public static ClientboundLobbyStatePacket decode(FriendlyByteBuf buf) {
        return new ClientboundLobbyStatePacket(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean(), buf.readUtf(), buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt));
    }

    public static void handle(ClientboundLobbyStatePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> LobbyClientState.set(msg));
        ctx.get().setPacketHandled(true);
    }
}
