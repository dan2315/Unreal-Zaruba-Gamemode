package com.dod.unrealzaruba.UI;

import java.util.List;

import com.dod.unrealzaruba.Economy.SupplyCaseClientState;
import com.dod.unrealzaruba.Schematics.PurchaseClientState;
import com.dod.unrealzaruba.Schematics.PurchaseGhostRenderer;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UI.Respawn.RespawnClientState;
import com.dod.unrealzaruba.UI.Vehicles.TeamVehiclesState;
import com.dod.unrealzaruba.UnrealZaruba;

/**
 * Everything the client holds for a match (objectives, score, timer, respawn state, vehicles, purchases, case beams, zones).
 * It is put away when the match ends, when the player leaves the game world and when they connect, so the lobby never shows
 * a leftover bar and a reconnect starts from nothing before the server sends what is current.
 */
public final class MatchHud {
    private MatchHud() {}

    public static void resetAll() {
        ObjectivesOverlay.INSTANCE.Clear();
        ScoreOverlay.INSTANCE.reset();
        TimerOverlay.OVERLAY_INSTANCE.stop();
        RespawnClientState.reset();
        TeamVehiclesState.reset();
        com.dod.unrealzaruba.UI.Feed.FeedState.clear();
        com.dod.unrealzaruba.UI.TabList.TeamRosterState.reset();
        PurchaseClientState.reset();
        PurchaseGhostRenderer.reset();
        SupplyCaseClientState.set(List.of());
        if (UnrealZaruba.geometryRenderer != null) UnrealZaruba.geometryRenderer.clear();
        com.dod.unrealzaruba.Radiopings.PingClientState.reset();
    }
}
