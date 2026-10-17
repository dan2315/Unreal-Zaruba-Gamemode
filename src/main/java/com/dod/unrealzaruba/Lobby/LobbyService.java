package com.dod.unrealzaruba.Lobby;

import java.util.Map;

import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.NetworkPackets.Lobby.ClientboundLobbyStatePacket;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Title.TitleMessage;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server side of the lobby screens: who is ready, the votes for the next mode, and what each player sees of it. */
public final class LobbyService {
    private LobbyService() {}

    /** Flips the readiness of the player (the /ready command and the ready button both come here). */
    public static void toggleReady(ServerPlayer player) {
        PlayerContext context = PlayerContext.Get(player.getUUID());
        if (context == null) return;
        context.SetReady(!context.IsReady());

        MinecraftServer server = UnrealZaruba.server;
        var players = server.getPlayerList().getPlayers();
        int minPlayers = ConfigManager.getMainConfig().getMinPlayersToStart();
        if (players.size() < minPlayers) {
            TitleMessage.sendActionbar(player, Component.literal("§6Чтобы начать игру, нужно минимум " + minPlayers + " игроков"));
        }
        broadcast();
    }

    public static void broadcast() {
        MinecraftServer server = UnrealZaruba.server;
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }

    public static void sendTo(ServerPlayer player) {
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
                myVote == null ? "" : myVote, votes));
    }
}
