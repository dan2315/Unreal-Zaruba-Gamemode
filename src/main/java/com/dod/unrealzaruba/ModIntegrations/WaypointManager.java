package com.dod.unrealzaruba.ModIntegrations;

import com.dod.unrealzaruba.UI.Objectives.HudCapturePointObjective;
import com.dod.unrealzaruba.UI.Objectives.HudDestroyObjective;
import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import com.dod.unrealzaruba.utils.Conversion;

import java.util.List;

public class WaypointManager {
    public static WaypointManager INSTANCE = new WaypointManager();
    private final HudCaptureCompassUpdate compassUpdate;
    private final XaeroMapUpdate xaeroMapUpdate;

    public WaypointManager() {
        compassUpdate = new HudCaptureCompassUpdate();
        xaeroMapUpdate = new XaeroMapUpdate();
    }

    public void SetupWaypoints(List<HudObjective> objectives) {
        xaeroMapUpdate.UpdateOrCreateMinimapWaypoints(objectives);
        for (var objective : objectives) {
             if (objective instanceof HudCapturePointObjective hudCapturePointObjective) {
                 compassUpdate.UpdateOrCreateCompassWaypoints(
                         hudCapturePointObjective.GetRuntimeId(),
                         hudCapturePointObjective.GetName(),
                         Conversion.BlockPosToVec3(hudCapturePointObjective.GetPosition()),
                         hudCapturePointObjective.getOwnerColor()
                 );
             } else if (objective instanceof HudDestroyObjective hudDestroyObjective) {
                 compassUpdate.UpdateOrCreateCompassWaypoints(
                         hudDestroyObjective.GetRuntimeId(),
                         hudDestroyObjective.GetName(),
                         Conversion.BlockPosToVec3(hudDestroyObjective.GetPosition()),
                         0x0000FF
                 );
             }
        }
    }
}
