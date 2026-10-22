package com.dod.unrealzaruba.UI.Feed;

import java.util.List;

import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.Feed.FeedKind;
import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.UiFont;
import com.dod.unrealzaruba.UI.ZarubaOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The event feed in the top right corner under the money: kills with the weapon, the colours of the teams and the reward, and the
 * events of the match (points taken, score marks, supply drops, vehicles). A line stays for a while; the whole history is on the TAB
 * panel. The corner is hidden while TAB is held, because the controls panel takes that place. The drawing of one line
 * ({@link #width}, {@link #draw}) is also used by the history.
 */
public class FeedOverlay implements ZarubaOverlay {
    public static final FeedOverlay INSTANCE = new FeedOverlay();

    private static final long KILL_MILLIS = 15_000;
    private static final long EVENT_MILLIS = 20_000;
    private static final long SLIDE_MILLIS = 180;
    /** How many fresh lines the corner shows at most. */
    private static final int SHOWN = 7;
    public static final int ROW_HEIGHT = 14;
    private static final int GAP = 2;
    private static final int PAD = 5;
    private static final int ICON = 12;

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        if (mc.options.keyPlayerList.isDown()) return;
        List<FeedState.Line> lines = FeedState.lines();
        if (lines.isEmpty()) return;

        Font font = UiFont.get();
        String me = mc.player.getGameProfile().getName();
        long now = System.currentTimeMillis();
        // Under the money panel, which has its own height
        int y = com.dod.unrealzaruba.UI.MoneyOverlay.INSTANCE.bottom(font, screenWidth) + 6;
        int right = screenWidth - HudStyle.MARGIN;

        // The newest few that have not expired
        int from = Math.max(0, lines.size() - SHOWN);
        for (int i = from; i < lines.size(); i++) {
            FeedState.Line line = lines.get(i);
            long age = now - line.createdAt();
            if (age > (line.kind() == FeedKind.KILL ? KILL_MILLIS : EVENT_MILLIS)) continue;
            int slide = (int) (Math.max(0, SLIDE_MILLIS - age) * 48 / SLIDE_MILLIS);
            draw(g, font, line, right + slide - width(font, line), y, me);
            y += ROW_HEIGHT + GAP;
        }
    }

    private static int rgb(int color) {
        return 0xFF000000 | (color & 0xFFFFFF);
    }

    /** The width of the drawn line. */
    public static int width(Font font, FeedState.Line line) {
        if (line.kind() == FeedKind.KILL) {
            boolean hasKiller = !line.nameA().isEmpty();
            String streak = line.streak() >= 2 ? " x" + line.streak() : "";
            int paid = line.reward() + line.bounty();
            String reward = paid > 0 ? "+" + Money.format(paid) : "";
            int width = PAD * 2 + ICON + 2;
            if (hasKiller) width += font.width(line.nameA()) + font.width(streak) + 4;
            width += font.width(line.nameB());
            if (!reward.isEmpty()) width += 6 + font.width(reward);
            return width;
        }
        return PAD * 2 + 2 + font.width(Component.translatable(line.key(), line.args().toArray()));
    }

    /** Draws a line with its left edge at {@code x}, {@link #ROW_HEIGHT} high. */
    public static void draw(GuiGraphics g, Font font, FeedState.Line line, int x, int y, String me) {
        if (line.kind() == FeedKind.KILL) {
            drawKill(g, font, line, x, y, me);
        } else {
            drawEvent(g, font, line, x, y);
        }
    }

    private static void drawKill(GuiGraphics g, Font font, FeedState.Line line, int x, int y, String me) {
        boolean hasKiller = !line.nameA().isEmpty();
        String streak = line.streak() >= 2 ? " x" + line.streak() : "";
        int paid = line.reward() + line.bounty();
        String reward = paid > 0 ? "+" + Money.format(paid) : "";
        int width = width(font, line);

        boolean mine = hasKiller && line.nameA().equals(me);
        boolean death = line.nameB().equals(me);
        g.fill(x, y, x + width, y + ROW_HEIGHT, Brand.alpha(Brand.WINDOW, 0xD0));
        g.fill(x, y, x + 2, y + ROW_HEIGHT, rgb(hasKiller ? line.colorA() : line.colorB()));
        if (mine) g.renderOutline(x, y, width, ROW_HEIGHT, Brand.WARN);
        else if (death) g.renderOutline(x, y, width, ROW_HEIGHT, 0xFFFF5C61);

        int cx = x + PAD;
        int textY = y + (ROW_HEIGHT - 8) / 2 + 1;
        if (hasKiller) {
            g.drawString(font, line.nameA(), cx, textY, rgb(line.colorA()), false);
            cx += font.width(line.nameA());
            if (!streak.isEmpty()) {
                g.drawString(font, streak, cx, textY, Brand.WARN, false);
                cx += font.width(streak);
            }
            cx += 4;
        }
        drawIcon(g, line, cx, y);
        cx += ICON + 2;
        g.drawString(font, line.nameB(), cx, textY, rgb(line.colorB()), false);
        cx += font.width(line.nameB());
        if (!reward.isEmpty()) {
            g.drawString(font, reward, cx + 6, textY, Brand.MINT, false);
        }
    }

    private static void drawEvent(GuiGraphics g, Font font, FeedState.Line line, int x, int y) {
        Component text = Component.translatable(line.key(), line.args().toArray());
        int width = width(font, line);
        g.fill(x, y, x + width, y + ROW_HEIGHT, Brand.alpha(Brand.WINDOW, 0xD0));
        g.fill(x, y, x + 2, y + ROW_HEIGHT, rgb(line.colorA()));
        g.drawString(font, text, x + PAD + 2, y + (ROW_HEIGHT - 8) / 2 + 1, Brand.TEXT, false);
    }

    private static void drawIcon(GuiGraphics g, FeedState.Line line, int x, int y) {
        if (line.icon().isEmpty()) return;
        g.pose().pushPose();
        g.pose().translate(x, y + (ROW_HEIGHT - ICON) / 2f, 0);
        g.pose().scale(ICON / 16f, ICON / 16f, 1f);
        g.renderItem(line.icon(), 0, 0);
        g.pose().popPose();
    }
}
