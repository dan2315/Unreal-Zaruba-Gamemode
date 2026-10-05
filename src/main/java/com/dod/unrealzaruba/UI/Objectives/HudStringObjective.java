package com.dod.unrealzaruba.UI.Objectives;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.network.FriendlyByteBuf;

import static com.dod.unrealzaruba.Gamemodes.Objectives.GameObjective.LastRuntimeId;

public class HudStringObjective extends HudObjective {
    public static final int TYPE_ID = 2;

    public String displayString;

    public HudStringObjective(String value) {
        this.runtimeId = LastRuntimeId++;
        displayString = value;
    }

    public HudStringObjective(FriendlyByteBuf buffer) {
        runtimeId = buffer.readByte();
        displayString = buffer.readUtf();
    }

    @Override
    public void Serialize(FriendlyByteBuf buffer) {
        buffer.writeVarInt(TYPE_ID);
        buffer.writeByte(runtimeId);
        buffer.writeUtf(displayString);
    }

    public String getText() {
        return displayString;
    }

    @Override
    public void Update(byte progress) {
        UnrealZaruba.LOGGER.error("Updating HUD element that is not meant to have progress: {}", progress);
    }
}
