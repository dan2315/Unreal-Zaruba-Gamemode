package com.dod.unrealzaruba.UI.Objectives;

import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.ScoreOverlay;
import com.dod.unrealzaruba.UI.UiFont;
import com.dod.unrealzaruba.UI.ZarubaOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The objectives while playing, kept minimal: under the score bar a row of skewed tiles with a letter inside. The tile
 * is in the colour of the team holding the point and fills with the colour of the team capturing it; a destroy target
 * fills as it gets destroyed. The full list is on the objectives panel, shown while TAB is held.
 */
public class ObjectivesStrip implements ZarubaOverlay {
    public static final ObjectivesStrip INSTANCE = new ObjectivesStrip();

    private static final int TILE_WIDTH = 24;
    private static final int TILE_HEIGHT = 14;
    private static final int SLANT = 5;
    private static final int GAP = 4;

    private record Tile(String letter, int base, int fillColor, float fill, boolean contested) {}

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        List<Tile> tiles = tiles();
        if (tiles.isEmpty()) return;
        Font font = UiFont.get();

        int total = tiles.size() * TILE_WIDTH + (tiles.size() - 1) * GAP + SLANT;
        int x = (screenWidth - total) / 2;
        int above = com.dod.unrealzaruba.UI.Compass.CompassOverlay.bottom(font, Minecraft.getInstance(), screenWidth);
        // Tiles float under the top panel, with no background of their own
        int y = above + 2;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 220.0);

        for (Tile tile : tiles) {
            HudStyle.skewedRect(g, x, y, TILE_WIDTH, TILE_HEIGHT, SLANT, Brand.alpha(Brand.WINDOW, 0xCC));
            if (tile.base() != 0) {
                HudStyle.skewedRect(g, x, y, TILE_WIDTH, TILE_HEIGHT, SLANT, Brand.alpha(tile.base(), 0x88));
            }
            if (tile.fill() > 0) {
                HudStyle.skewedRectFilled(g, x, y, TILE_WIDTH, TILE_HEIGHT, SLANT, tile.fill(), Brand.alpha(tile.fillColor(), 0xCC));
            }
            int border = tile.contested() ? Brand.alpha(Brand.WARN, 0x66 + (int) (0x99 * pulse))
                    : tile.base() != 0 ? tile.base() : Brand.BORDER;
            HudStyle.skewedOutline(g, x, y, TILE_WIDTH, TILE_HEIGHT, SLANT, border);

            String letter = tile.letter();
            Component label = Component.literal(letter).withStyle(UiFont.bold());
            int textX = x + SLANT / 2 + (TILE_WIDTH - font.width(label)) / 2;
            g.drawString(font, label, textX, y + (TILE_HEIGHT - 8) / 2 + 1, Brand.TEXT, true);
            x += TILE_WIDTH + GAP;
        }
    }

    /** True when the row is drawn (the panels above it and their borders depend on it). */
    public static boolean isShown() {
        return !tiles().isEmpty();
    }

    private static List<Tile> tiles() {
        List<Tile> tiles = new ArrayList<>();
        int destroyIndex = 0;
        for (HudObjective objective : ObjectivesOverlay.INSTANCE.objectives()) {
            if (objective instanceof HudCapturePointObjective point) {
                String letter = point.isRespawnOnly() ? "⌂" : ObjectivesOverlay.INSTANCE.letterFor(point.GetRuntimeId());
                boolean capturing = point.getCapturedBy() != null && point.getCapturedBy() != point.getOwner() && point.getProgress() > 0;
                tiles.add(new Tile(letter == null ? "?" : letter,
                        point.getOwner() == null ? 0 : HudStyle.teamColor(point.getOwner()),
                        capturing ? HudStyle.teamColor(point.getCapturedBy()) : 0,
                        capturing ? point.getProgress() : 0, capturing));
            } else if (objective instanceof HudDestroyObjective target) {
                tiles.add(new Tile(String.valueOf((char) ('A' + destroyIndex++ % 26)), 0, HudStyle.RED,
                        Math.max(0, Math.min(1, target.getProgress())), false));
            }
        }
        return tiles;
    }
}
