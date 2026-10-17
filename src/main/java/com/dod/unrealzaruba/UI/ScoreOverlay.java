package com.dod.unrealzaruba.UI;

import com.dod.unrealzaruba.UI.Compass.CompassOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;

/** Team score panel at the top center: red score, a bar filling outward from the middle toward the goal, blue score. */
public class ScoreOverlay implements ZarubaOverlay {
    public static final ScoreOverlay INSTANCE = new ScoreOverlay();

    private static final int WIDTH = 190;
    private static final float SCORE_SCALE = 1.4f;

    boolean visible;
    short leftScore;
    short rightScore;
    short goal = 200;

    @Override
    public void render(Gui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!visible) return;

        Font font = com.dod.unrealzaruba.UI.UiFont.get();
        int scoreHeight = Math.round(font.lineHeight * SCORE_SCALE) - 1;
        int height = scoreHeight + 8;
        int x = (screenWidth - WIDTH) / 2;
        // The compass strip lives at the very top; drop the score panel below it when visible.
        int y = HudStyle.MARGIN;
        if (CompassOverlay.shouldRender(Minecraft.getInstance())) {
            y = CompassOverlay.topOffset();
        }
        HudStyle.panel(guiGraphics, x, y, WIDTH, height);

        int scoreBox = HudStyle.width(font, "000", SCORE_SCALE);
        int textY = y + 4;
        String left = String.valueOf(leftScore);
        String right = String.valueOf(rightScore);
        HudStyle.text(guiGraphics, font, left, x + 7 + scoreBox - HudStyle.width(font, left, SCORE_SCALE), textY, HudStyle.RED, SCORE_SCALE);
        HudStyle.text(guiGraphics, font, right, x + WIDTH - 7 - scoreBox, textY, HudStyle.BLUE, SCORE_SCALE);

        int barLeft = x + 7 + scoreBox + 6;
        int barRight = x + WIDTH - 7 - scoreBox - 6;
        int barY = y + height / 2 - 1;
        int center = (barLeft + barRight) / 2;
        int half = center - barLeft;
        guiGraphics.fill(barLeft, barY, barRight, barY + 3, HudStyle.BAR_BG);
        guiGraphics.fill(center - fill(leftScore, half), barY, center, barY + 3, HudStyle.RED);
        guiGraphics.fill(center, barY, center + fill(rightScore, half), barY + 3, HudStyle.BLUE);
        guiGraphics.fill(center, barY, center + 1, barY + 3, HudStyle.ON_COLOR);
    }

    private int fill(int score, int half) {
        if (goal <= 0) return 0;
        return Math.min(half, half * Math.max(0, score) / goal);
    }

    public void SetScore(byte id, short score, short goal) {
        if (goal > 0) this.goal = goal;
        switch (id) {
            case 0:
                leftScore = score;
                break;
            case 1:
                rightScore = score;
                break;
        }
    }

    /** Where the panel ends (or where it would start while hidden), for what is drawn under it. */
    public int bottom(Font font) {
        if (!visible) return HudStyle.MARGIN - 4;
        return HudStyle.MARGIN + Math.round(font.lineHeight * SCORE_SCALE) - 1 + 8;
    }

    public void SetVisible(boolean visible) {
        this.visible = visible;
    }
}
