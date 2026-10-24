package com.dod.unrealzaruba.Economy;

import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.AbstractGamemodeData;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;

/**
 * Per-map economy config (economy.json in the gamemode's data folder). A map without the file gets the defaults
 * written out, and an existing file is written back so settings added later show up in it.
 */
public class EconomyData extends AbstractGamemodeData<EconomyConfig> {
    private static final String DATA_NAME = "economy";

    public EconomyData(Class<? extends BaseGamemode> gamemodeClass) {
        super(EconomyConfig.class, gamemodeClass, DATA_NAME, EconomyConfig.createDefault());
    }

    @Override
    public EconomyConfig loadData() {
        EconomyConfig config = super.loadData();
        if (config.buyMenu == null || config.buyMenu.isEmpty()) {
            config.buyMenu = EconomyConfig.createDefault().buyMenu; // a file with no menu gets the stock one
        }
        DefaultBuyMenu.addMissing(config); // new stock entries reach maps that were saved before them
        saveData();
        return config;
    }

    /** Config of the running map, or the defaults outside a match. */
    public static EconomyConfig current() {
        BaseGamemode gamemode = GamemodeManager.instance.GetActiveGamemode();
        if (gamemode != null) {
            EconomyData data = GamemodeDataManager.getHandler(gamemode.getClass(), EconomyData.class);
            if (data != null && data.getData() != null) {
                return data.getData();
            }
        }
        return EconomyConfig.createDefault();
    }
}
