package com.dod.unrealzaruba.Gamemodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.Comparator;

import com.dod.unrealzaruba.Economy.Wallets;
import com.dod.unrealzaruba.WorldManager.GameMaps;
import com.dod.unrealzaruba.Title.TitleMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.UnrealZaruba;


public class GamemodeManager {
    public static GamemodeManager instance;
    private BaseGamemode activeGamemode;

    // Map key per player
    private HashMap<UUID, String> playerVotes = new HashMap<>();
    private String forcedMap;
    private boolean isVoting = true;
    private int lobbyTicks;

    public BaseGamemode GetActiveGamemode() {
        return activeGamemode;
    }

    public void ForGamemode(Consumer<BaseGamemode> action) {
        if (activeGamemode != null) {
            action.accept(activeGamemode);
        }
    }

    public GamemodeManager() {}

    public void StartVoting() {
        isVoting = true;
        com.dod.unrealzaruba.Lobby.LobbyService.broadcast();
    }

    public boolean IsVoting() {
        return isVoting;
    }

    /** Votes per map key. */
    public java.util.Map<String, Integer> VoteCounts() {
        java.util.Map<String, Integer> counts = new HashMap<>();
        playerVotes.values().forEach(vote -> counts.merge(vote, 1, Integer::sum));
        return counts;
    }

    public String VoteOf(UUID playerId) {
        return playerVotes.get(playerId);
    }

    /** A vote for a map (by its key; a mode id counts for that mode's first map). */
    public int Vote(UUID playerId, String vote) {
        if (!isVoting) {
            return 0;
        }
        GameMaps.GameMap map = GameMaps.find(vote);
        if (map == null) {
            return 0;
        }

        playerVotes.put(playerId, map.key());
        com.dod.unrealzaruba.Lobby.LobbyService.broadcast();
        return 1;
    }

    public void Tick() {

        if (!isVoting) {
            return;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        
        var players = server.getPlayerList().getPlayers();
        if (players.size() == 0) {
            return;
        }
        // Joins, leaves and ready changes reach the lobby screens within a second
        if (++lobbyTicks % 20 == 0) {
            com.dod.unrealzaruba.Lobby.LobbyService.broadcast();
        }

        boolean allPlayersReady = true;
        List<String> notReadyPlayers = new ArrayList<>();
        
        for (ServerPlayer player : players) {
            PlayerContext playerContext = PlayerContext.Get(player.getUUID());
            if (playerContext == null || !playerContext.IsReady()) {
                allPlayersReady = false;
                notReadyPlayers.add(player.getName().getString());
            }
        }

        if (allPlayersReady && players.size() >= ConfigManager.getMainConfig().getMinPlayersToStart()) {
            StartGame();
        }
    }

    public void StartGame() {
        StopVoting();
        ResetPlayersReady();
    }

    /** /forcestart &lt;map&gt;: the match starts on this map whatever the votes say. */
    public void StartGame(GameMaps.GameMap map) {
        forcedMap = map.key();
        StartGame();
    }

    private void ResetPlayersReady() {
        for (ServerPlayer player : UnrealZaruba.server.getPlayerList().getPlayers()) {
            PlayerContext playerContext = PlayerContext.Get(player.getUUID());
            playerContext.SetReady(false);
        }
    }

    public void StopVoting() {
        UnrealZaruba.LOGGER.warn("Stop Voting has been called");
        isVoting = false;
        HashMap<String, Integer> votes = new HashMap<>();
        playerVotes.forEach((playerId, vote) -> {
            votes.put(vote, votes.getOrDefault(vote, 0) + 1);
        });

        String mostVoted = forcedMap != null ? forcedMap : votes.keySet().stream()
            .max(Comparator.comparingInt(votes::get))
            .orElse(null);
        forcedMap = null;
        GameMaps.GameMap map = GameMaps.get(mostVoted);
        if (map == null) {
            map = GameMaps.fallback(); // nobody voted, or the voted map is gone
        }

        UnrealZaruba.LOGGER.warn("Voting: {} has won", map == null ? "nothing" : map.key());

        if (map != null) {
            GameMaps.select(map.key());
            GamemodeFactory.createGamemode(map.mode());
        } else {
            GamemodeFactory.createGamemode(CapturePointsGamemode.GAMEMODE_NAME);
        }

        playerVotes.clear();
        com.dod.unrealzaruba.Lobby.LobbyService.broadcast();
    }

    public void SetActiveGamemode(BaseGamemode gamemode) {
        activeGamemode = gamemode;
    }

    public void CleanupCurrentGamemode() {
        activeGamemode = null;
        com.dod.unrealzaruba.Vehicles.PurchaseSessions.reset();
        com.dod.unrealzaruba.Vehicles.TrackedVehicles.reset();
        Wallets.clear(ServerLifecycleHooks.getCurrentServer());
        StartVoting();
    }
}
