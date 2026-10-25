package com.dod.unrealzaruba.UI;

import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.Events.KeyBindings;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Under the money panel during a match: the shop and team storage with their keys ("Shop [B]"), each with a small icon (a cart, a
 * container). A line is green where it works, in the team's respawn zones, and grey elsewhere. The server says which.
 */
public class ZoneHintsOverlay implements ZarubaOverlay {
    public static final ZoneHintsOverlay INSTANCE = new ZoneHintsOverlay();

    /** A line that works where the player stands. */
    private static final int ON = 0xFF5FD068;
    private static final int GAP = 4;
    private static final int PAD = 5;
    private static final int ICON_WIDTH = 11;

    private static final String[] CART = {
            "##.........",
            ".#.........",
            ".##########",
            "..#.#.#.#.#",
            "..#########",
            "...#.#.#.#.",
            "...#######.",
            "....#...#..",
    };
    private static final String[] CONTAINER = {
            "###########",
            "#.#.#.#.#.#",
            "#.#.#.#.#.#",
            "#.#.#.#.#.#",
            "#.#.#.#.#.#",
            "#.#.#.#.#.#",
            "###########",
    };

    private record Row(String[] icon, String text, boolean on) {}

    private boolean shown;
    private boolean shopEnabled;
    private boolean shopOpen;
    private boolean storageOpen;

    public void set(boolean shown, boolean shopEnabled, boolean shopOpen, boolean storageOpen) {
        this.shown = shown;
        this.shopEnabled = shopEnabled;
        this.shopOpen = shopOpen;
        this.storageOpen = storageOpen;
    }

    public void reset() {
        set(false, false, false, false);
    }

    private boolean visible(Minecraft mc) {
        return shown && MoneyOverlay.INSTANCE.isVisible() && mc.player != null && mc.level != null && mc.screen == null
                && !mc.options.keyPlayerList.isDown();
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        if (shopEnabled) rows.add(new Row(CART, line("hud.unrealzaruba.hint.shop", KeyBindings.OPEN_BUY_MENU), shopOpen));
        rows.add(new Row(CONTAINER, line("hud.unrealzaruba.hint.storage", KeyBindings.OPEN_TEAM_STORAGE), storageOpen));
        return rows;
    }

    private static String line(String key, KeyMapping binding) {
        return Component.translatable(key).getString() + " [" + TipsOverlay.englishKeyName(binding) + "]";
    }

    private static int lineHeight(Font font) {
        return font.lineHeight + 3;
    }

    private static int width(Font font, List<Row> rows) {
        int text = 0;
        for (Row row : rows) text = Math.max(text, font.width(row.text()));
        return PAD + ICON_WIDTH + 4 + text + PAD;
    }

    private static int height(Font font, List<Row> rows) {
        return PAD + rows.size() * lineHeight(font) - 3 + PAD;
    }

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc)) return;
        Font font = UiFont.get();
        List<Row> rows = rows();
        int width = width(font, rows);
        int x = screenWidth - HudStyle.MARGIN - width;
        int y = MoneyOverlay.INSTANCE.bottom(font, screenWidth) + GAP;
        HudStyle.panel(g, x, y, width, height(font, rows));
        int cy = y + PAD;
        for (Row row : rows) {
            int color = row.on() ? ON : HudStyle.DIM;
            icon(g, x + PAD, cy + (font.lineHeight - 1 - row.icon().length) / 2, row.icon(), color);
            g.drawString(font, row.text(), x + PAD + ICON_WIDTH + 4, cy, color, false);
            cy += lineHeight(font);
        }
    }

    /** Pixel art: '#' is a pixel of the colour. */
    private static void icon(GuiGraphics g, int x, int y, String[] rows, int color) {
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length(); c++) {
                if (rows[r].charAt(c) == '#') g.fill(x + c, y + r, x + c + 1, y + r + 1, color);
            }
        }
    }

    /** Where the panel ends (for the lines of the feed under it), or where the money panel ends while it is hidden. */
    public int bottom(Font font, int screenWidth) {
        int money = MoneyOverlay.INSTANCE.bottom(font, screenWidth);
        return visible(Minecraft.getInstance()) ? money + GAP + height(font, rows()) : money;
    }
}
