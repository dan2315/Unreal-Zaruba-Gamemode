package com.dod.unrealzaruba.UI.TabList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Feed.Feed;
import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.Compass.CompassOverlay;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesStrip;
import com.dod.unrealzaruba.UI.UiFont;
import com.dod.unrealzaruba.UI.ZarubaOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * The player list on TAB, in place of the vanilla one: two columns with the headings RED and BLUE, below the match HUD so
 * that it does not lie on it. Players in no team (lobby, spectators) are listed under the columns; before any team exists
 * the list is a single column.
 */
public class TabListOverlay implements ZarubaOverlay {
    public static final TabListOverlay INSTANCE = new TabListOverlay();

    private static final int ROW = 11;
    private static final int HEADER = 14;
    private static final int GAP = 10;

    private record Entry(String name, int ping, boolean me) {}

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || !mc.options.keyPlayerList.isDown()) return;
        var connection = mc.getConnection();
        if (connection == null) return;

        List<Entry> red = new ArrayList<>();
        List<Entry> blue = new ArrayList<>();
        List<Entry> rest = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            Entry entry = new Entry(info.getProfile().getName(), info.getLatency(), info.getProfile().getId().equals(mc.player.getUUID()));
            TeamColor team = TeamRosterState.teamOf(info.getProfile().getId());
            if (team == TeamColor.RED) red.add(entry);
            else if (team == TeamColor.BLUE) blue.add(entry);
            else rest.add(entry);
        }
        Comparator<Entry> byName = Comparator.comparing(e -> e.name().toLowerCase());
        red.sort(byName);
        blue.sort(byName);
        rest.sort(byName);

        Font font = UiFont.get();
        // Under the match HUD (score, compass, the row of points), not over it
        int above = CompassOverlay.bottom(font, mc, screenWidth);
        if (ObjectivesStrip.isShown()) above += 2 + 14;
        int top = Math.max(HudStyle.MARGIN, above) + 10;
        int room = Math.max(4, (screenHeight - top - 24) / ROW - 3);

        if (TeamRosterState.any()) {
            int colWidth = Math.max(130, Math.min(170, (screenWidth - 2 * HudStyle.MARGIN - GAP) / 2));
            int x0 = (screenWidth - (2 * colWidth + GAP)) / 2;
            int rows = Math.min(room, Math.max(red.size(), blue.size()));
            int height = column(g, font, x0, top, colWidth, "RED", Feed.colorOf(TeamColor.RED), red, rows);
            column(g, font, x0 + colWidth + GAP, top, colWidth, "BLUE", Feed.colorOf(TeamColor.BLUE), blue, rows);
            if (!rest.isEmpty()) {
                int restRows = Math.min(rest.size(), Math.max(2, room - rows));
                column(g, font, x0, top + height + GAP, 2 * colWidth + GAP, "NO TEAM", 0x8D97A3, rest, restRows);
            }
        } else {
            int colWidth = Math.max(160, Math.min(220, screenWidth / 3));
            column(g, font, (screenWidth - colWidth) / 2, top, colWidth, "PLAYERS", 0x8D97A3, rest, Math.min(rest.size(), room + 6));
        }
    }

    /** One column: a heading in the colour of the team and the players, as many as {@code rows}. Returns its height. */
    private static int column(GuiGraphics g, Font font, int x, int y, int width, String title, int color, List<Entry> players, int rows) {
        int height = HEADER + Math.max(1, rows) * ROW + 4;
        g.fill(x, y, x + width, y + height, Brand.alpha(Brand.WINDOW, 0xD8));
        g.renderOutline(x, y, width, height, HudStyle.PANEL_BORDER);
        g.fill(x, y, x + width, y + HEADER, 0xCC000000 | (color & 0xFFFFFF));
        g.fill(x, y + HEADER - 1, x + width, y + HEADER, 0x66000000);
        String count = String.valueOf(players.size());
        g.drawString(font, title, x + 6, y + (HEADER - 8) / 2 + 1, 0xFFFFFFFF, true);
        g.drawString(font, count, x + width - 6 - font.width(count), y + (HEADER - 8) / 2 + 1, 0xFFFFFFFF, true);

        int shown = Math.min(rows, players.size());
        for (int i = 0; i < shown; i++) {
            Entry entry = players.get(i);
            int ry = y + HEADER + 2 + i * ROW;
            if (entry.me()) g.fill(x + 2, ry - 1, x + width - 2, ry + ROW - 1, 0x33FFFFFF);
            g.drawString(font, HudStyle.trim(font, entry.name(), width - 50), x + 6, ry, entry.me() ? Brand.WARN : Brand.TEXT, false);
            String ping = entry.ping() + "ms";
            int pingColor = entry.ping() < 80 ? Brand.OK : entry.ping() < 160 ? Brand.WARN : 0xFFFF5C61;
            g.drawString(font, ping, x + width - 6 - font.width(ping), ry, pingColor, false);
        }
        if (players.size() > shown) {
            String more = "+" + (players.size() - shown);
            g.drawString(font, more, x + 6, y + HEADER + 2 + shown * ROW, Brand.TEXT_MUTE, false);
        }
        return height;
    }
}
