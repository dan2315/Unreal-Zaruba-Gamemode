package com.dod.unrealzaruba.ModIntegrations;

import com.dod.unrealzaruba.ModIntegrations.Xaero.XaeroWaypoints;
import com.dod.unrealzaruba.ModIntegrations.Xaero.XaeroWorldMap;

import net.neoforged.fml.ModList;

/**
 * Client entry point for Xaero's mods, which are optional: their classes are only touched when installed.
 * The minimap holds the waypoints, the world map is the respawn picker.
 */
public class WaypointManager {
    public static final WaypointManager INSTANCE = new WaypointManager();
    private static final int SYNC_INTERVAL_TICKS = 10;

    private final XaeroWaypoints waypoints;
    private final XaeroWorldMap worldMap;
    private int ticks;

    private WaypointManager() {
        waypoints = ModList.get().isLoaded("xaerominimap") ? new XaeroWaypoints() : null;
        worldMap = waypoints != null && ModList.get().isLoaded("xaeroworldmap") ? new XaeroWorldMap(waypoints) : null;
    }

    public XaeroWaypoints waypoints() {
        return waypoints;
    }

    public XaeroWorldMap worldMap() {
        return worldMap;
    }

    public void tick() {
        if (waypoints == null || ++ticks % SYNC_INTERVAL_TICKS != 0) return;
        if (waypoints.sync() && worldMap != null) {
            worldMap.refresh();
        }
    }

    /** False when the world map isn't available, so the caller falls back to the death screen. */
    public boolean openMapForRespawn() {
        if (waypoints != null) waypoints.sync();
        return worldMap != null && worldMap.openForRespawn();
    }

    public void closeMapAfterRespawn() {
        if (worldMap != null) worldMap.closeAfterRespawn();
    }
}
