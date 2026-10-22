package com.dod.unrealzaruba.UI.Respawn;

import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsView;

/** What the client knows about its respawn: the team's points from the server, and the death countdown. */
public final class RespawnClientState {
    private static RespawnPointsView view = RespawnPointsView.EMPTY;
    private static boolean dead;
    /** The waiting is over and a respawn point has to be chosen (while it is not, the camera flies over the body). */
    private static boolean choosing;
    private static int secondsLeft;
    private static net.minecraft.core.BlockPos supplyDrop;

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

    public static boolean isChoosing() {
        return dead && choosing;
    }

    public static void setDead(boolean isDead, int seconds) {
        dead = isDead;
        choosing = isDead && seconds <= 0;
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

    /** The unopened supply crate, shown on the map, or null. */
    public static net.minecraft.core.BlockPos supplyDrop() {
        return supplyDrop;
    }

    public static void setSupplyDrop(net.minecraft.core.BlockPos pos) {
        supplyDrop = pos;
    }

    public static void reset() {
        supplyDrop = null;
        view = RespawnPointsView.EMPTY;
        dead = false;
        choosing = false;
        secondsLeft = 0;
    }
}
