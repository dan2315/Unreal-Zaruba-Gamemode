package com.dod.unrealzaruba.NetworkPackets.Lobby;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.Lobby.LobbyClientState;

import net.minecraft.network.FriendlyByteBuf;

/**
 * What the lobby screens show: whether voting is on, readiness counts, the own state of the player, the maps to vote
 * for and the votes per map.
 */
public class ClientboundLobbyStatePacket implements ModPacket {
    /** A map in the vote. Name and description may be empty: then the mode's are shown. */
    public record MapCard(String key, String mode, String name, String description) {}

    public final boolean active;
    public final int ready;
    public final int players;
    public final int minPlayers;
    public final boolean myReady;
    public final String myVote;
    public final Map<String, Integer> votes;
    public final List<MapCard> maps;

    public ClientboundLobbyStatePacket(boolean active, int ready, int players, int minPlayers, boolean myReady,
                                       String myVote, Map<String, Integer> votes, List<MapCard> maps) {
        this.active = active;
        this.ready = ready;
        this.players = players;
        this.minPlayers = minPlayers;
        this.myReady = myReady;
        this.myVote = myVote;
        this.votes = votes;
        this.maps = maps;
    }

    public static void encode(ClientboundLobbyStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeVarInt(msg.ready);
        buf.writeVarInt(msg.players);
        buf.writeVarInt(msg.minPlayers);
        buf.writeBoolean(msg.myReady);
        buf.writeUtf(msg.myVote);
        buf.writeMap(msg.votes, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
        buf.writeCollection(msg.maps, (b, map) -> {
            b.writeUtf(map.key());
            b.writeUtf(map.mode());
            b.writeUtf(map.name());
            b.writeUtf(map.description());
        });
    }

    public static ClientboundLobbyStatePacket decode(FriendlyByteBuf buf) {
        return new ClientboundLobbyStatePacket(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean(), buf.readUtf(), buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt),
                buf.readList(b -> new MapCard(b.readUtf(), b.readUtf(), b.readUtf(), b.readUtf())));
    }

    public static void handle(ClientboundLobbyStatePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> LobbyClientState.set(msg));
        ctx.get().setPacketHandled(true);
    }
}
