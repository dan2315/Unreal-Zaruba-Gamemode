package com.dod.unrealzaruba.UI.TabList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Feed.Feed;
import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.Compass.CompassOverlay;
import com.dod.unrealzaruba.UI.Feed.FeedOverlay;
import com.dod.unrealzaruba.UI.Feed.FeedState;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesStrip;
import com.dod.unrealzaruba.UI.UiFont;
import com.dod.unrealzaruba.UI.ZarubaOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/**
 * The player list on TAB, in place of the vanilla one: two columns with the headings RED and BLUE, below the match HUD so
 * that it does not lie on it, the load of the server above the columns, and under them the history of the event feed, which the
 * mouse wheel scrolls (see {@link TabListScroll}). Players in no team (lobby, spectators) are listed under the columns; before any
 * team exists the list is a single column.
 */
public class TabListOverlay implements ZarubaOverlay {
    public static final TabListOverlay INSTANCE = new TabListOverlay();

    private static final int ROW = 11;
    private static final int HEADER = 14;
    private static final int GAP = 10;
    private static final int HISTORY_ROW = FeedOverlay.ROW_HEIGHT + 1;
    private static final int AGE_WIDTH = 32;
    /** Room for the ping (right aligned at the edge) and for each of the columns of kills and deaths. */
    private static final int PING_WIDTH = 38;
    private static final int STAT_WIDTH = 24;

    private record Entry(String name, int ping, boolean me, int kills, int deaths) {}

    private static boolean wasDown;
    /** How many history lines the panel shows now, for the scrolling. */
    private static volatile int historyRows = 8;

    public static int historyRows() {
        return historyRows;
    }

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        boolean down = mc.options.keyPlayerList.isDown();
        if (!down) {
            if (wasDown) FeedState.resetScroll(); // the next time it opens at the newest line
            wasDown = false;
            return;
        }
        wasDown = true;
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        var connection = mc.getConnection();
        if (connection == null) return;

        List<Entry> red = new ArrayList<>();
        List<Entry> blue = new ArrayList<>();
        List<Entry> rest = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            Entry entry = new Entry(info.getProfile().getName(), info.getLatency(), info.getProfile().getId().equals(mc.player.getUUID()),
                    TeamRosterState.kills(info.getProfile().getId()), TeamRosterState.deaths(info.getProfile().getId()));
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
        // Under the match HUD (score, compass, the row of points), not over it; a line above the columns is for the server load
        int above = CompassOverlay.bottom(font, mc, screenWidth);
        if (ObjectivesStrip.isShown()) above += 2 + 14;
        int top = Math.max(HudStyle.MARGIN, above) + 22;
        int room = Math.max(3, (screenHeight - top - 24) / ROW - 8);

        int panelX;
        int panelWidth;
        int bottom;
        if (TeamRosterState.any()) {
            int colWidth = Math.max(130, Math.min(170, (screenWidth - 2 * HudStyle.MARGIN - GAP) / 2));
            panelWidth = 2 * colWidth + GAP;
            panelX = (screenWidth - panelWidth) / 2;
            int rows = Math.min(room, Math.max(red.size(), blue.size()));
            int height = column(g, font, panelX, top, colWidth, "RED", Feed.colorOf(TeamColor.RED), red, rows);
            column(g, font, panelX + colWidth + GAP, top, colWidth, "BLUE", Feed.colorOf(TeamColor.BLUE), blue, rows);
            bottom = top + height;
            if (!rest.isEmpty()) {
                int restRows = Math.min(rest.size(), Math.max(2, room - rows));
                bottom += GAP + column(g, font, panelX, bottom + GAP, panelWidth, "NO TEAM", 0x8D97A3, rest, restRows);
            }
        } else {
            panelWidth = Math.max(300, Math.min(340, screenWidth - 2 * HudStyle.MARGIN));
            panelX = (screenWidth - panelWidth) / 2;
            int colWidth = Math.max(160, Math.min(220, screenWidth / 3));
            bottom = top + column(g, font, (screenWidth - colWidth) / 2, top, colWidth, "PLAYERS", 0x8D97A3, rest, Math.min(rest.size(), room + 3));
        }
        serverLoad(g, font, panelX + panelWidth, top - 12);
        history(g, font, panelX, bottom + GAP, panelWidth, screenHeight, mc.player.getGameProfile().getName());
    }

    /** The load of the server, right aligned: ticks per second and the time of a tick, green, yellow or red. */
    private static void serverLoad(GuiGraphics g, Font font, int right, int y) {
        float tps = TeamRosterState.tps();
        if (tps < 0) return;
        int color = tps >= 18.5f ? Brand.OK : tps >= 15f ? Brand.WARN : 0xFFFF5C61;
        String text = String.format(java.util.Locale.ROOT, "TPS %.1f  %.1f ms", tps, TeamRosterState.mspt());
        g.fill(right - font.width(text) - 10, y - 2, right, y + 10, Brand.alpha(Brand.WINDOW, 0xD0));
        g.fill(right - font.width(text) - 10, y - 2, right - font.width(text) - 8, y + 10, color);
        g.drawString(font, text, right - font.width(text) - 4, y, color, false);
    }

    /** The history of the event feed, newest at the bottom; the wheel scrolls it. */
    private static void history(GuiGraphics g, Font font, int x, int y, int width, int screenHeight, String me) {
        List<FeedState.Line> lines = FeedState.lines();
        if (lines.isEmpty()) return;
        int rows = Math.max(3, Math.min(10, (screenHeight - y - HEADER - 26) / HISTORY_ROW));
        historyRows = rows;
        int scroll = FeedState.scrollBy(0, rows);
        int end = lines.size() - scroll;
        int start = Math.max(0, end - rows);
        int height = HEADER + rows * HISTORY_ROW + 4;

        g.fill(x, y, x + width, y + height, Brand.alpha(Brand.WINDOW, 0xD8));
        g.renderOutline(x, y, width, height, HudStyle.PANEL_BORDER);
        g.fill(x, y, x + width, y + HEADER, 0xCC2C3540);
        g.drawString(font, "EVENTS", x + 6, y + (HEADER - 8) / 2 + 1, 0xFFFFFFFF, true);
        if (lines.size() > rows) {
            Component hint = Component.translatable("hud.unrealzaruba.tab.feed_hint");
            g.drawString(font, hint, x + width - 6 - font.width(hint), y + (HEADER - 8) / 2 + 1, Brand.TEXT_MUTE, false);
        }

        int rowsTop = y + HEADER + 2;
        g.enableScissor(x + 1, rowsTop, x + width - 6, rowsTop + rows * HISTORY_ROW);
        long now = System.currentTimeMillis();
        for (int i = start; i < end; i++) {
            FeedState.Line line = lines.get(i);
            int ry = rowsTop + (i - start) * HISTORY_ROW;
            long seconds = Math.max(0, (now - line.createdAt()) / 1000);
            g.drawString(font, String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60), x + 5, ry + 3, Brand.TEXT_MUTE, false);
            FeedOverlay.draw(g, font, line, x + 6 + AGE_WIDTH, ry, me);
        }
        g.disableScissor();

        // The scroll bar, when there is more than the panel shows
        if (lines.size() > rows) {
            int trackHeight = rows * HISTORY_ROW;
            int thumb = Math.max(10, trackHeight * rows / lines.size());
            int maxScroll = lines.size() - rows;
            int thumbTop = rowsTop + (trackHeight - thumb) * (maxScroll - scroll) / Math.max(1, maxScroll);
            g.fill(x + width - 5, rowsTop, x + width - 2, rowsTop + trackHeight, 0x66000000);
            g.fill(x + width - 5, thumbTop, x + width - 2, thumbTop + thumb, Brand.TEXT_DIM);
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
        int headerY = y + (HEADER - 8) / 2 + 1;
        g.drawString(font, title + "  " + count, x + 6, headerY, 0xFFFFFFFF, true);
        // The columns of kills and deaths sit left of the ping
        int deathsRight = x + width - 6 - PING_WIDTH;
        int killsRight = deathsRight - STAT_WIDTH;
        g.drawString(font, "K", killsRight - font.width("K"), headerY, 0xFFFFFFFF, true);
        g.drawString(font, "D", deathsRight - font.width("D"), headerY, 0xFFFFFFFF, true);

        int shown = Math.min(rows, players.size());
        for (int i = 0; i < shown; i++) {
            Entry entry = players.get(i);
            int ry = y + HEADER + 2 + i * ROW;
            if (entry.me()) g.fill(x + 2, ry - 1, x + width - 2, ry + ROW - 1, 0x33FFFFFF);
            g.drawString(font, HudStyle.trim(font, entry.name(), width - 12 - PING_WIDTH - 2 * STAT_WIDTH), x + 6, ry, entry.me() ? Brand.WARN : Brand.TEXT, false);
            if (entry.kills() >= 0) {
                String kills = String.valueOf(entry.kills());
                String deaths = String.valueOf(entry.deaths());
                g.drawString(font, kills, killsRight - font.width(kills), ry, Brand.TEXT, false);
                g.drawString(font, deaths, deathsRight - font.width(deaths), ry, Brand.TEXT_DIM, false);
            }
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
