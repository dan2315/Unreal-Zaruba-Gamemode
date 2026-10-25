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
 * Under the money panel during a match, only where the player can use them (the team's respawn zones; the server says which):
 * the shop and team storage with their keys ("Shop [B]") and that leaving the game there costs nothing, each with a small icon
 * (a cart, a container, a door). Elsewhere the panel is not shown.
 */
public class ZoneHintsOverlay implements ZarubaOverlay {
    public static final ZoneHintsOverlay INSTANCE = new ZoneHintsOverlay();

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
    private static final String[] DOOR = {
            "..#######..",
            "..#.....#..",
            "..#.....#..",
            "..#...###..",
            "..#.....#..",
            "..#.....#..",
            "..#.....#..",
            "###########",
    };

    private record Row(String[] icon, String text) {}

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

    /** Whether the player stands in their team's own zone, where leaving the game costs nothing. */
    public boolean inOwnZone() {
        return shown && storageOpen;
    }

    private boolean visible(Minecraft mc) {
        return shown && MoneyOverlay.INSTANCE.isVisible() && mc.player != null && mc.level != null && mc.screen == null
                && !mc.options.keyPlayerList.isDown() && !rows().isEmpty();
    }

    /** The lines that apply where the player stands: the team zone gives storage and a free way out; the shop may work anywhere. */
    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        if (shopEnabled && shopOpen) rows.add(new Row(CART, line("hud.unrealzaruba.hint.shop", KeyBindings.OPEN_BUY_MENU)));
        if (storageOpen) {
            rows.add(new Row(CONTAINER, line("hud.unrealzaruba.hint.storage", KeyBindings.OPEN_TEAM_STORAGE)));
            rows.add(new Row(DOOR, Component.translatable("hud.unrealzaruba.hint.safe_logout").getString()));
        }
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
            icon(g, x + PAD, cy + (font.lineHeight - 1 - row.icon().length) / 2, row.icon(), ON);
            g.drawString(font, row.text(), x + PAD + ICON_WIDTH + 4, cy, ON, false);
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
