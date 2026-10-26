package com.dod.unrealzaruba.Economy;

import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.AbstractGamemodeData;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;

/**
 * Where automatic supply drops land on this map (supplydroparea.json in the map's data folder): the area and the axis
 * along which they lean toward the team that is behind. The rest of the drop settings belong to the mode
 * (economy.json). A map without its own area uses the one in economy.json, as before maps had their own.
 */
public class SupplyAreaData extends AbstractGamemodeData<SupplyAreaData.Payload> {
    private static final String DATA_NAME = "supplydroparea";

    public static class Payload {
        public EconomyConfig.Area area;
        /** X or Z; null to use the axis in economy.json. */
        public String biasAxis;
    }

    public SupplyAreaData(Class<? extends BaseGamemode> gamemodeClass) {
        super(Payload.class, gamemodeClass, DATA_NAME, new Payload());
    }

    // A setting of the map, kept in its data folder
    @Override
    protected boolean perMap() {
        return true;
    }

    /** The running map's settings, or null outside a match. */
    public static SupplyAreaData current() {
        BaseGamemode gamemode = GamemodeManager.instance == null ? null : GamemodeManager.instance.GetActiveGamemode();
        return gamemode == null ? null : GamemodeDataManager.getHandler(gamemode.getClass(), SupplyAreaData.class);
    }

    /** The drop area of the running map: its own when it has one, else the one in economy.json. */
    public static EconomyConfig.Area area(EconomyConfig.Paradrop paradrop) {
        SupplyAreaData data = current();
        Payload own = data == null ? null : data.getData();
        return own != null && own.area != null ? own.area : paradrop.area;
    }

    public static String biasAxis(EconomyConfig.Paradrop paradrop) {
        SupplyAreaData data = current();
        Payload own = data == null ? null : data.getData();
        return own != null && own.biasAxis != null ? own.biasAxis : paradrop.biasAxis;
    }

    /** The map's own area, started from the one it uses now, for /supplydrop area to change. */
    public EconomyConfig.Area ownArea(EconomyConfig.Paradrop paradrop) {
        if (data.area == null) {
            EconomyConfig.Area copy = new EconomyConfig.Area();
            copy.x1 = paradrop.area.x1;
            copy.z1 = paradrop.area.z1;
            copy.x2 = paradrop.area.x2;
            copy.z2 = paradrop.area.z2;
            copy.floorY = paradrop.area.floorY;
            data.area = copy;
        }
        return data.area;
    }
}
