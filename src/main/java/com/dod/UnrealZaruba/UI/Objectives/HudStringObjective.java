package com.dod.unrealzaruba.UI.Objectives;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    @Override
    public void Render(GuiGraphics guiGraphics, int x, int[] yReference) {
        Minecraft mc = Minecraft.getInstance();

        int y = yReference[0] + 10;     // vertical offset

        guiGraphics.drawString(mc.font, displayString, x, y, 0xFFFFFF);
    }

    @Override
    public void Update(byte progress) {
        UnrealZaruba.LOGGER.error("Updating HUD element that is not meant to have progress: {}", progress);
    }
}
