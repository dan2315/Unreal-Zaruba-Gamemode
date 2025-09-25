package com.dod.unrealzaruba.Gamemodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.Comparator;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class GamemodeManager {
    public static GamemodeManager instance;
    private BaseGamemode activeGamemode;

    private HashMap<UUID, String> playerVotes = new HashMap<>();
    private boolean isVoting = true;

    public BaseGamemode GetActiveGamemode() {
        return activeGamemode;
    }

    public void ForGamemode(Consumer<BaseGamemode> action) {
        if (activeGamemode != null) {
            action.accept(activeGamemode);
        }
    }

    public GamemodeManager() {
    }

    public void StartVoting() {
        isVoting = true;
    }

    public int Vote(UUID playerId, String vote) {
        if (!isVoting) {
            return 0;
        }

        playerVotes.put(playerId, vote);
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

        boolean allPlayersReady = true;
        List<String> notReadyPlayers = new ArrayList<>();
        
        for (ServerPlayer player : players) {
            PlayerContext playerContext = PlayerContext.Get(player.getUUID());
            if (playerContext == null || !playerContext.IsReady()) {
                allPlayersReady = false;
                notReadyPlayers.add(player.getName().getString());
            }
        }

        if (allPlayersReady) {
            StopVoting();
            ResetPlayersReady(players);
        }
    }

    private void ResetPlayersReady(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
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

        String mostVoted = votes.keySet().stream()
            .max(Comparator.comparingInt(votes::get))
            .orElse(CapturePointsGamemode.GAMEMODE_NAME);

        UnrealZaruba.LOGGER.warn("Voting: {} has won", mostVoted);

        if (mostVoted != null) {
            GamemodeFactory.createGamemode(mostVoted);
        }

        playerVotes.clear();
    }

    public void SetActiveGamemode(BaseGamemode gamemode) {
        activeGamemode = gamemode;
    }

    public void CleanupCurrentGamemode() {
        activeGamemode = null;
        StartVoting();
    }
}
