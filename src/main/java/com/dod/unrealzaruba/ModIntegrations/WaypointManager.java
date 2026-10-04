package com.dod.unrealzaruba.ModIntegrations;

import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import net.neoforged.fml.ModList;

import java.util.List;

public class WaypointManager {
    public static WaypointManager INSTANCE = new WaypointManager();
    // Xaero's Minimap is optional; only touch its classes when it is installed.
    private final XaeroMapUpdate xaeroMapUpdate;

    public WaypointManager() {
        xaeroMapUpdate = ModList.get().isLoaded("xaerominimap") ? new XaeroMapUpdate() : null;
    }

    public void SetupWaypoints(List<HudObjective> objectives) {
        if (xaeroMapUpdate != null) {
            xaeroMapUpdate.UpdateOrCreateMinimapWaypoints(objectives);
        }
    }
}
