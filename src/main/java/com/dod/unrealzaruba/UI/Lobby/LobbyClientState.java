package com.dod.unrealzaruba.UI.Lobby;

import java.util.Map;

import com.dod.unrealzaruba.NetworkPackets.Lobby.ClientboundLobbyStatePacket;

/** The lobby as the server described it last. */
public final class LobbyClientState {
    private static boolean active;
    private static int ready;
    private static int players;
    private static int minPlayers;
    private static boolean myReady;
    private static String myVote = "";
    private static Map<String, Integer> votes = Map.of();

    private LobbyClientState() {}

    public static void set(ClientboundLobbyStatePacket packet) {
        active = packet.active;
        ready = packet.ready;
        players = packet.players;
        minPlayers = packet.minPlayers;
        myReady = packet.myReady;
        myVote = packet.myVote;
        votes = packet.votes;
    }

    public static void setMyVote(String mode) {
        myVote = mode;
    }

    public static void reset() {
        active = false;
        ready = 0;
        players = 0;
        myReady = false;
        myVote = "";
        votes = Map.of();
    }

    public static boolean active() { return active; }
    public static int ready() { return ready; }
    public static int players() { return players; }
    public static int minPlayers() { return minPlayers; }
    public static boolean myReady() { return myReady; }
    public static String myVote() { return myVote; }
    public static int votesFor(String mode) { return votes.getOrDefault(mode, 0); }

    /** The mode with the most votes, or null while nobody has voted. */
    public static String leader() {
        String best = null;
        int bestVotes = 0;
        for (var entry : votes.entrySet()) {
            if (entry.getValue() > bestVotes) {
                best = entry.getKey();
                bestVotes = entry.getValue();
            }
        }
        return best;
    }
}
