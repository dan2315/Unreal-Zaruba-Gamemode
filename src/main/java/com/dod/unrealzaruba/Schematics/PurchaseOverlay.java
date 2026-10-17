package com.dod.unrealzaruba.Schematics;

import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.UiFont;
import com.dod.unrealzaruba.UI.ZarubaOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** A joint purchase near the player, over the hotbar: what is bought, how much is paid and the seconds left. */
public class PurchaseOverlay implements ZarubaOverlay {
    public static final PurchaseOverlay INSTANCE = new PurchaseOverlay();

    private static final double RANGE = 40;
    private static final int WIDTH = 200;

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || PurchaseClientState.all().isEmpty()) return;

        PurchaseClientState.View nearest = null;
        double best = RANGE * RANGE;
        for (PurchaseClientState.View view : PurchaseClientState.all()) {
            double distance = minecraft.player.distanceToSqr(view.pos().getCenter());
            if (distance < best) {
                best = distance;
                nearest = view;
            }
        }
        if (nearest == null) return;

        Font font = UiFont.get();
        int x = (screenWidth - WIDTH) / 2;
        int y = screenHeight - 78;
        g.fill(x, y, x + WIDTH, y + 30, Brand.alpha(Brand.WINDOW, 0xD9));
        g.renderOutline(x, y, WIDTH, 30, Brand.BORDER);
        g.drawString(font, HudStyle.trim(font, nearest.name(), WIDTH - 50), x + 8, y + 5, Brand.TEXT, false);
        String time = Component.translatable("schematics.unrealzaruba.purchase.seconds", nearest.secondsLeft()).getString();
        g.drawString(font, time, x + WIDTH - 8 - font.width(time), y + 5, nearest.secondsLeft() <= 3 ? Brand.WARN : Brand.TEXT_DIM, false);
        g.drawString(font, Money.format(nearest.paid()) + " / " + Money.format(nearest.price()), x + 8, y + 17, Brand.TEXT_DIM, false);
        int bar = Math.round((WIDTH - 16) * nearest.fraction());
        g.fill(x + 8, y + 27, x + WIDTH - 8, y + 29, Brand.BAR_TRACK);
        if (bar > 0) Brand.gradient(g, x + 8, y + 27, bar, 2, Brand.MINT_DIM, Brand.CYAN_DIM, Brand.BLUE);
    }
}
