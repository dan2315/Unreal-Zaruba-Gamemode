package com.dod.unrealzaruba.UI;

import com.dod.unrealzaruba.Economy.Money;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

/** Player's dollars in a panel in the top right corner during a match. Values come from the server. */
public class MoneyOverlay implements ZarubaOverlay {
    public static final MoneyOverlay INSTANCE = new MoneyOverlay();
    private static final float SCALE = 1.5f;

    private boolean visible = false;
    private int balance = 0;

    public void set(boolean visible, int balance) {
        this.visible = visible;
        this.balance = balance;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getBalance() {
        return balance;
    }

    @Override
    public void render(Gui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible || mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }

        Font font = com.dod.unrealzaruba.UI.UiFont.get();
        String text = Money.format(balance);
        int textWidth = HudStyle.width(font, text, SCALE);
        int textHeight = Math.round(font.lineHeight * SCALE) - 1;
        int width = textWidth + 16;
        int height = textHeight + 8;
        int x = screenWidth - HudStyle.MARGIN - width;
        int y = top(font, mc, screenWidth, width);
        HudStyle.panel(guiGraphics, x, y, width, height);
        HudStyle.text(guiGraphics, font, text, x + 8, y + 4, HudStyle.ACCENT, SCALE);
    }

    /** The height of the panel for a balance. */
    private static int panelHeight(Font font) {
        return Math.round(font.lineHeight * SCALE) - 1 + 8;
    }

    /**
     * Where the panel starts: in the corner, unless the screen is so narrow that the match panel at the top centre reaches the corner, and
     * then under the match panel.
     */
    private static int top(Font font, Minecraft mc, int screenWidth, int width) {
        int topPanelRight = (screenWidth + HudStyle.topWidth(screenWidth)) / 2;
        if (screenWidth - HudStyle.MARGIN - width >= topPanelRight + 4) return HudStyle.MARGIN;
        int above = com.dod.unrealzaruba.UI.Compass.CompassOverlay.bottom(font, mc, screenWidth);
        if (com.dod.unrealzaruba.UI.Objectives.ObjectivesStrip.isShown()) above += 2 + 14;
        return Math.max(HudStyle.MARGIN, above + 4);
    }

    /** Where the panel ends (for the lines of the feed under it), or where it would end while it is hidden. */
    public int bottom(Font font, int screenWidth) {
        Minecraft mc = Minecraft.getInstance();
        String text = Money.format(balance);
        int width = HudStyle.width(font, text, SCALE) + 16;
        return top(font, mc, screenWidth, width) + panelHeight(font);
    }
}
