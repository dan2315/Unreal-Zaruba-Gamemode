package com.dod.unrealzaruba.Lobby;

import java.util.List;
import java.util.Map;

import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.WorldManager.GameMaps;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.NetworkPackets.Lobby.ClientboundLobbyStatePacket;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server side of the lobby screens: who is ready, the maps and the votes for the next one, and what each player sees of it. */
public final class LobbyService {
    private LobbyService() {}

    /** Flips the readiness of the player (the /ready command and the ready button both come here). */
    public static void toggleReady(ServerPlayer player) {
        PlayerContext context = PlayerContext.Get(player.getUUID());
        if (context == null) return;
        context.SetReady(!context.IsReady());
        // The minimum number of players is on the lobby panel in the top right corner
        broadcast();
    }

    public static void broadcast() {
        MinecraftServer server = UnrealZaruba.server;
        if (server == null) return;
        List<ClientboundLobbyStatePacket.MapCard> maps = maps();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player, maps);
        }
    }

    public static void sendTo(ServerPlayer player) {
        sendTo(player, maps());
    }

    /** The maps to vote for (read once per send, not per player). */
    private static List<ClientboundLobbyStatePacket.MapCard> maps() {
        return GameMaps.all().stream()
                .map(map -> new ClientboundLobbyStatePacket.MapCard(map.key(), map.mode(), map.name(), map.description()))
                .toList();
    }

    private static void sendTo(ServerPlayer player, List<ClientboundLobbyStatePacket.MapCard> maps) {
        MinecraftServer server = UnrealZaruba.server;
        GamemodeManager manager = GamemodeManager.instance;
        if (server == null || manager == null) return;

        var players = server.getPlayerList().getPlayers();
        int ready = 0;
        for (ServerPlayer other : players) {
            PlayerContext context = PlayerContext.Get(other.getUUID());
            if (context != null && context.IsReady()) ready++;
        }
        PlayerContext own = PlayerContext.Get(player.getUUID());
        Map<String, Integer> votes = manager.VoteCounts();
        String myVote = manager.VoteOf(player.getUUID());
        PacketDistributor.sendToPlayer(player, new ClientboundLobbyStatePacket(manager.IsVoting(), ready, players.size(),
                ConfigManager.getMainConfig().getMinPlayersToStart(), own != null && own.IsReady(),
                myVote == null ? "" : myVote, votes, maps));
    }
}
