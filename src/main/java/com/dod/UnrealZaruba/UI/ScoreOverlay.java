package com.dod.unrealzaruba.UI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class ScoreOverlay implements IGuiOverlay {
    public static final ScoreOverlay INSTANCE = new ScoreOverlay();

    boolean visible;
    short leftScore;
    short rightScore;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!visible) return;

        int widthUnit = screenWidth / 10;
        int heightUnit = screenHeight / 10;

        // Получаем шрифт от gui
        var font = gui.getFont();

        // Немного отступаем от центра
        int y = heightUnit/2;
        int leftX = screenWidth / 2 - 3*widthUnit;
        int rightX = screenWidth / 2 + 3*widthUnit;

        // Левый счёт (красный)
        guiGraphics.drawString(
                font,
                String.valueOf(leftScore),
                leftX,
                y,
                0xFF0000, // красный
                true      // тень
        );

        // Правый счёт (синий)
        guiGraphics.drawString(
                font,
                String.valueOf(rightScore),
                rightX,
                y,
                0x0000FF, // синий
                true      // тень
        );
    }

    public void SetScore(byte id, short score) {
        switch (id) {
            case 0:
                leftScore = score;
                break;
            case 1:
                rightScore =score;
                break;
        }
    }

    public void SetVisible(boolean visible) {
        this.visible = visible;
    }
}
