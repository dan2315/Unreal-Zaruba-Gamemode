package com.dod.unrealzaruba.UI.Respawn;

import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsView;

/** What the client knows about its respawn: the team's points from the server, and the death countdown. */
public final class RespawnClientState {
    private static RespawnPointsView view = RespawnPointsView.EMPTY;
    private static boolean dead;
    private static int secondsLeft;

    private RespawnClientState() {}

    public static RespawnPointsView view() {
        return view;
    }

    public static void setView(RespawnPointsView newView) {
        view = newView != null ? newView : RespawnPointsView.EMPTY;
    }

    public static boolean isDead() {
        return dead;
    }

    public static int secondsLeft() {
        return secondsLeft;
    }

    public static void setDead(boolean isDead, int seconds) {
        dead = isDead;
        secondsLeft = seconds;
    }

    public static void setSecondsLeft(int seconds) {
        secondsLeft = seconds;
    }

    /** Name of a respawn point, with its capture point letter when it has one. */
    public static String describe(RespawnPointsView.Entry entry) {
        if (entry == null) return "—";
        if (entry.objectiveId() != null) {
            String letter = com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay.INSTANCE.letterFor(entry.objectiveId());
            if (letter != null) return entry.name() + " (" + letter + ")";
        }
        return entry.name();
    }

    public static void reset() {
        view = RespawnPointsView.EMPTY;
        dead = false;
        secondsLeft = 0;
    }
}
