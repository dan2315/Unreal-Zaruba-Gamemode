package com.dod.UnrealZaruba.UI.Objectives;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;

public class HudCapturePointObjective extends HudObjective {
    public static final int TYPE_ID = 1;

    private final String name;
    private final int ownerColor; // as background
    private final int capturedByColor; // as progress line
    private float progress;


    public HudCapturePointObjective(FriendlyByteBuf buffer) {
        this.runtimeId = buffer.readByte();
        this.name = buffer.readUtf();
        this.ownerColor = buffer.readVarInt();
        this.capturedByColor = buffer.readVarInt();
        this.progress = buffer.readFloat();
    }

    public HudCapturePointObjective(Byte runtimeId, String name, int ownerColor, int capturedByColor, float progress) {
        this.runtimeId = runtimeId;
        this.name = name;
        this.ownerColor = ownerColor;
        this.capturedByColor = capturedByColor;
        this.progress = progress;
    }

    @Override
    public void Serialize(FriendlyByteBuf buffer) {
        buffer.writeVarInt(TYPE_ID);
        buffer.writeByte(runtimeId);
        buffer.writeUtf(name);
        buffer.writeVarInt(ownerColor);
        buffer.writeVarInt(capturedByColor);
        buffer.writeFloat(progress);
    }

    @Override
    public void Render(GuiGraphics guiGraphics, int x, int[] yReference) {
        Minecraft mc = Minecraft.getInstance();

        int y = yReference[0] + 10;     // vertical offset

        guiGraphics.drawString(mc.font, name, x ,y ,0xFFFFFF);

        int barX = x;
        int barY = y + 12;
        int barWidth = 100;
        int barHeight = 8;

        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF000000 | ownerColor);

        int filled = (int) (barWidth * progress);
        guiGraphics.fill(barX, barY, barX + filled, barY + barHeight, 0xFF000000 | capturedByColor);

        yReference[0] = barY + barHeight + 4;
    }

    @Override
    public void Update(byte progress) {
        this.progress = (progress - Byte.MIN_VALUE) / (float)(Byte.MAX_VALUE - Byte.MIN_VALUE);
    }
}
