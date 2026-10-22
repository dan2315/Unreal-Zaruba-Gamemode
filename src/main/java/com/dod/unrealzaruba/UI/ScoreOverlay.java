package com.dod.unrealzaruba.UI;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;

/** Team score panel at the top center: red score, a bar filling outward from the middle toward the goal, blue score. */
public class ScoreOverlay implements ZarubaOverlay {
    public static final ScoreOverlay INSTANCE = new ScoreOverlay();

    private static final float SCORE_SCALE = 1.0f;

    boolean visible;
    short leftScore;
    short rightScore;
    short goal = 200;

    @Override
    public void render(Gui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!visible) return;

        Font font = com.dod.unrealzaruba.UI.UiFont.get();
        int scoreHeight = Math.round(font.lineHeight * SCORE_SCALE) - 1;
        int height = scoreHeight + 6;
        int width = HudStyle.topWidth(screenWidth);
        int x = (screenWidth - width) / 2;
        int y = 0; // glued to the top edge
        HudStyle.dockedPanel(guiGraphics, x, y, width, height, !com.dod.unrealzaruba.UI.Compass.CompassOverlay.shouldRender(net.minecraft.client.Minecraft.getInstance()));

        int scoreBox = HudStyle.width(font, "000", SCORE_SCALE);
        int textY = y + 3;
        String left = String.valueOf(leftScore);
        String right = String.valueOf(rightScore);
        HudStyle.text(guiGraphics, font, left, x + 7 + scoreBox - HudStyle.width(font, left, SCORE_SCALE), textY, HudStyle.RED, SCORE_SCALE);
        HudStyle.text(guiGraphics, font, right, x + width - 7 - scoreBox, textY, HudStyle.BLUE, SCORE_SCALE);

        // The rate of the score: as many arrows as the lead in held points, on the side of the team that leads
        int arrowBox = ARROW_STEP * MAX_ARROWS + 2;
        int lead = pointsHeld(com.dod.unrealzaruba.Commands.Arguments.TeamColor.RED) - pointsHeld(com.dod.unrealzaruba.Commands.Arguments.TeamColor.BLUE);
        int arrowsY = y + height / 2 - 3;
        for (int i = 0; i < Math.min(MAX_ARROWS, Math.abs(lead)); i++) {
            if (lead > 0) arrow(guiGraphics, x + 7 + scoreBox + 4 + (MAX_ARROWS - 1 - i) * ARROW_STEP, arrowsY, true, HudStyle.RED);
            else arrow(guiGraphics, x + width - 7 - scoreBox - 4 - arrowBox + 2 + i * ARROW_STEP, arrowsY, false, HudStyle.BLUE);
        }

        int barLeft = x + 7 + scoreBox + 6 + arrowBox;
        int barRight = x + width - 7 - scoreBox - 6 - arrowBox;
        int barY = y + height / 2 - 1;
        int center = (barLeft + barRight) / 2;
        int half = center - barLeft;
        guiGraphics.fill(barLeft, barY, barRight, barY + 3, HudStyle.BAR_BG);
        guiGraphics.fill(center - fill(leftScore, half), barY, center, barY + 3, HudStyle.RED);
        guiGraphics.fill(center, barY, center + fill(rightScore, half), barY + 3, HudStyle.BLUE);
        guiGraphics.fill(center, barY, center + 1, barY + 3, HudStyle.ON_COLOR);
    }

    private static final int ARROW_STEP = 5;
    private static final int MAX_ARROWS = 4;

    /** Capture points a team holds now, from what the objectives say. */
    private static int pointsHeld(com.dod.unrealzaruba.Commands.Arguments.TeamColor team) {
        int count = 0;
        for (var objective : com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay.INSTANCE.objectives()) {
            if (objective instanceof com.dod.unrealzaruba.UI.Objectives.HudCapturePointObjective point && point.getOwner() == team) count++;
        }
        return count;
    }

    /** A small chevron, 3 wide and 7 tall, pointing left or right. */
    private static void arrow(GuiGraphics g, int x, int y, boolean left, int color) {
        for (int i = 0; i < 4; i++) {
            int col = left ? x + i : x + 3 - i;
            g.fill(col, y + 3 - i, col + 1, y + 4 + i, color);
        }
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
        if (!visible) return 0;
        return Math.round(font.lineHeight * SCORE_SCALE) - 1 + 6;
    }

    public boolean isShown() {
        return visible;
    }

    /** Back to nothing shown: the match is over or the player is somewhere else. */
    public void reset() {
        visible = false;
        leftScore = 0;
        rightScore = 0;
    }

    public void SetVisible(boolean visible) {
        this.visible = visible;
    }
}
