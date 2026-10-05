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

        Font font = mc.font;
        String text = Money.format(balance);
        int textWidth = HudStyle.width(font, text, SCALE);
        int textHeight = Math.round(font.lineHeight * SCALE) - 1;
        int width = textWidth + 16;
        int height = textHeight + 8;
        int x = screenWidth - HudStyle.MARGIN - width;
        int y = HudStyle.MARGIN;
        HudStyle.panel(guiGraphics, x, y, width, height);
        HudStyle.text(guiGraphics, font, text, x + 8, y + 4, HudStyle.ACCENT, SCALE);
    }
}
