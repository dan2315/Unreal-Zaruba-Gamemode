package com.dod.UnrealZaruba.Gamemodes.Barriers;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.AbstractGamemodeData;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;

import java.util.ArrayList;
import java.util.List;

public class BarrierVolumesData extends AbstractGamemodeData<BarrierVolumesData.BarriersPayload> {
    private static final String DATA_NAME = "barriervolumes";

    public BarrierVolumesData(Class<? extends BaseGamemode> gamemodeClass) {
        super(BarriersPayload.class, gamemodeClass, DATA_NAME, new BarriersPayload());
    }

    public Class<BarrierVolumesData.BarriersPayload> getDataClass() {
        return BarrierVolumesData.BarriersPayload.class;
    }

    public void AddBarrier(BlockVolume blockVolume) {
        data.barriers.add(blockVolume);
        saveData();
    }

    public static class BarriersPayload
    {
        List<BlockVolume> barriers = new ArrayList<>();

        public void setBarriers(List<BlockVolume> barriers) {
            this.barriers = barriers;
        }

        public List<BlockVolume> getBarriers() {
            return barriers;
        }
    }
}
