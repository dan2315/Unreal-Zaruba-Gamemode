package com.dod.unrealzaruba.UI.BuyMenu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.dod.unrealzaruba.Economy.BuyMenuView;
import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.Events.KeyBindings;
import com.dod.unrealzaruba.NetworkPackets.Economy.BuyItemsPacket;
import com.dod.unrealzaruba.UI.MoneyOverlay;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Buy menu (B). "grid" tabs: categories on the left, item cards in the middle, details and the buy button on the
 * right. "sets" tabs: set cards in the middle, the chosen set's pieces with toggles on the right.
 * Everything shown comes from the server; the server re-checks prices on purchase.
 */
public class BuyMenuScreen extends Screen {
    private static final int BG = 0xF00D1014;
    private static final int PANEL = 0xFF12161B;
    private static final int SIDE = 0xFF10141A;
    private static final int BORDER = 0xFF262D36;
    private static final int BORDER_LIGHT = 0xFF2F3843;
    private static final int CARD = 0xFF151A20;
    private static final int CARD_SELECTED = 0xFF1F2227;
    private static final int TAB_ACTIVE = 0xFF1A2027;
    private static final int ICON_BG = 0xFF1A2028;
    private static final int ICON_EDGE = 0xFF161B22;
    private static final int BAR_BG = 0xFF232A33;
    private static final int TEXT = 0xFFE8ECEF;
    private static final int MUTED = 0xFF9AA6B2;
    private static final int ACCENT = 0xFFF2A33A;
    private static final int ON_ACCENT = 0xFF14110A;
    private static final int SUCCESS = 0xFF6FCF7F;
    private static final int ERROR = 0xFFE5676B;

    private static final int HEADER = 26;
    private static final int LEFT = 108;
    private static final int RIGHT = 152;
    private static final int GAP = 6;
    private static final int MIN_CARD_WIDTH = 92;
    private static final int CARD_HEIGHT = 56;
    private static final int SET_CARD_HEIGHT = 66;
    private static final int RESULT_TICKS = 60;

    private record Hit(int x0, int y0, int x1, int y1, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    /** A set on a "sets" tab: categories of all its groups, flattened. */
    private record SetRef(int group, int category, BuyMenuView.Category data) {}

    private final BuyMenuView view;
    private final List<Hit> hits = new ArrayList<>();

    private int tab;
    private int group;
    private int category;
    private int entry;
    private int set;
    /** Unticked pieces of the selected set. */
    private final Set<Integer> piecesOff = new HashSet<>();
    private int scroll;
    private int maxScroll;

    private Component result;
    private boolean resultSuccess;
    private int resultTicks;

    private ItemStack hoveredStack = ItemStack.EMPTY;

    public BuyMenuScreen(BuyMenuView view) {
        super(Component.translatable("buymenu.unrealzaruba.title"));
        this.view = view;
        selectTab(0);
    }

    // ---------- state ----------

    private BuyMenuView.Tab currentTab() {
        return tab < view.tabs().size() ? view.tabs().get(tab) : null;
    }

    private BuyMenuView.Category currentCategory() {
        BuyMenuView.Tab t = currentTab();
        if (t == null || group >= t.groups().size()) return null;
        List<BuyMenuView.Category> categories = t.groups().get(group).categories();
        return category < categories.size() ? categories.get(category) : null;
    }

    private BuyMenuView.Entry currentEntry() {
        BuyMenuView.Category c = currentCategory();
        return c != null && entry < c.entries().size() ? c.entries().get(entry) : null;
    }

    private List<SetRef> sets() {
        List<SetRef> sets = new ArrayList<>();
        BuyMenuView.Tab t = currentTab();
        if (t == null) return sets;
        for (int g = 0; g < t.groups().size(); g++) {
            List<BuyMenuView.Category> categories = t.groups().get(g).categories();
            for (int c = 0; c < categories.size(); c++) {
                sets.add(new SetRef(g, c, categories.get(c)));
            }
        }
        return sets;
    }

    private void selectTab(int index) {
        tab = index;
        scroll = 0;
        piecesOff.clear();
        set = 0;
        group = 0;
        category = 0;
        entry = 0;
        // Start on the first category that has something in it
        BuyMenuView.Tab t = currentTab();
        if (t == null) return;
        for (int g = 0; g < t.groups().size(); g++) {
            List<BuyMenuView.Category> categories = t.groups().get(g).categories();
            for (int c = 0; c < categories.size(); c++) {
                if (!categories.get(c).entries().isEmpty()) {
                    group = g;
                    category = c;
                    return;
                }
            }
        }
    }

    private void selectCategory(int g, int c) {
        group = g;
        category = c;
        entry = 0;
        scroll = 0;
    }

    private void selectSet(int index) {
        set = index;
        piecesOff.clear();
    }

    public void showResult(boolean success, Component message) {
        result = message;
        resultSuccess = success;
        resultTicks = RESULT_TICKS;
    }

    private int balance() {
        return MoneyOverlay.INSTANCE.getBalance();
    }

    private void buy(int g, int c, List<Integer> entries) {
        PacketDistributor.sendToServer(new BuyItemsPacket(tab, g, c, entries));
    }

    // ---------- screen ----------

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (resultTicks > 0 && --resultTicks == 0) {
            result = null;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, BG);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        hits.clear();
        hoveredStack = ItemStack.EMPTY;

        renderHeader(g);
        BuyMenuView.Tab t = currentTab();
        if (t == null) {
            g.drawCenteredString(font, Component.translatable("buymenu.unrealzaruba.empty"), width / 2, height / 2, MUTED);
        } else if (t.isSets()) {
            renderSets(g, mouseX, mouseY);
        } else {
            renderGrid(g, mouseX, mouseY);
        }

        if (!hoveredStack.isEmpty()) {
            g.renderTooltip(font, hoveredStack, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (hit.contains(mouseX, mouseY)) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) (scrollY * 16), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.OPEN_BUY_MENU.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---------- header ----------

    private void renderHeader(GuiGraphics g) {
        g.fill(0, 0, width, HEADER, PANEL);
        g.fill(0, HEADER - 1, width, HEADER, BORDER);

        int textY = (HEADER - 8) / 2;
        Component title = Component.translatable("buymenu.unrealzaruba.title").withStyle(ChatFormatting.BOLD);
        g.drawString(font, title, 10, textY, TEXT, false);

        int x = 10 + font.width(title) + 16;
        for (int i = 0; i < view.tabs().size(); i++) {
            String name = upper(view.tabs().get(i).name());
            int w = font.width(name) + 20;
            boolean on = i == tab;
            if (on) {
                g.fill(x, 3, x + w, HEADER - 1, TAB_ACTIVE);
                g.fill(x, HEADER - 3, x + w, HEADER - 1, ACCENT);
            }
            g.drawString(font, name, x + 10, textY, on ? TEXT : MUTED, false);
            int index = i;
            hits.add(new Hit(x, 0, x + w, HEADER, () -> selectTab(index)));
            x += w + 2;
        }

        String money = Money.format(balance());
        int moneyX = width - 10 - font.width(money);
        g.drawString(font, money, moneyX, textY, ACCENT, false);
        String label = upper(Component.translatable("buymenu.unrealzaruba.balance").getString());
        int labelX = moneyX - 6 - font.width(label);
        g.drawString(font, label, labelX, textY, MUTED, false);
        String esc = Component.translatable("buymenu.unrealzaruba.esc").getString();
        int escX = labelX - 14 - font.width(esc);
        if (escX > x + 8) {
            g.drawString(font, esc, escX, textY, MUTED, false);
        }
    }

    // ---------- grid tab ----------

    private void renderGrid(GuiGraphics g, int mouseX, int mouseY) {
        BuyMenuView.Tab t = currentTab();

        // categories
        g.fill(0, HEADER, LEFT, height, SIDE);
        g.fill(LEFT - 1, HEADER, LEFT, height, BORDER);
        int y = HEADER + 6;
        for (int gi = 0; gi < t.groups().size(); gi++) {
            BuyMenuView.Group grp = t.groups().get(gi);
            drawSmall(g, upper(grp.name()), 8, y + 2, MUTED);
            y += 11;
            for (int ci = 0; ci < grp.categories().size(); ci++) {
                BuyMenuView.Category cat = grp.categories().get(ci);
                boolean on = gi == group && ci == category;
                if (on) {
                    g.fill(4, y, LEFT - 5, y + 14, CARD_SELECTED);
                    g.renderOutline(4, y, LEFT - 9, 14, ACCENT);
                }
                String count = String.valueOf(cat.entries().size());
                g.drawString(font, trim(upper(cat.name()), LEFT - 26 - font.width(count)), 9, y + 3, on ? ACCENT : TEXT, false);
                g.drawString(font, count, LEFT - 9 - font.width(count), y + 3, MUTED, false);
                int gIndex = gi, cIndex = ci;
                hits.add(new Hit(4, y, LEFT - 5, y + 14, () -> selectCategory(gIndex, cIndex)));
                y += 15;
            }
            y += 4;
        }

        // cards
        BuyMenuView.Category cat = currentCategory();
        int left = LEFT + 10;
        int right = width - RIGHT - 10;
        if (cat == null) return;

        g.drawString(font, upper(cat.name()), left, HEADER + 8, TEXT, false);
        g.drawString(font, trim(text(cat.hint()), right - left), left, HEADER + 20, MUTED, false);

        int top = HEADER + 34;
        int areaWidth = right - left;
        int cols = Math.max(1, (areaWidth + GAP) / (MIN_CARD_WIDTH + GAP));
        int cardWidth = (areaWidth - GAP * (cols - 1)) / cols;
        int rows = (cat.entries().size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (CARD_HEIGHT + GAP) - GAP - (height - 8 - top));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        g.enableScissor(left, top, right, height - 8);
        for (int i = 0; i < cat.entries().size(); i++) {
            BuyMenuView.Entry e = cat.entries().get(i);
            int cx = left + (i % cols) * (cardWidth + GAP);
            int cy = top + (i / cols) * (CARD_HEIGHT + GAP) - scroll;
            if (cy + CARD_HEIGHT < top || cy > height) continue;

            boolean on = i == entry;
            g.fill(cx, cy, cx + cardWidth, cy + CARD_HEIGHT, on ? CARD_SELECTED : CARD);
            g.renderOutline(cx, cy, cardWidth, CARD_HEIGHT, on ? ACCENT : BORDER);
            iconBox(g, cx + 4, cy + 4, cardWidth - 8, 26, e.stack());

            String price = Money.format(e.price());
            g.drawString(font, price, cx + cardWidth - 5 - font.width(price), cy + 34, ACCENT, false);
            g.drawString(font, trim(entryName(e), cardWidth - 14 - font.width(price)), cx + 5, cy + 34, e.available() ? TEXT : MUTED, false);
            drawSmall(g, trim(text(e.note()), (int) ((cardWidth - 10) / 0.75f)), cx + 5, cy + 46, MUTED);

            boolean visible = mouseY >= top && mouseY < height - 8;
            if (visible && mouseX >= cx + 4 && mouseX < cx + cardWidth - 4 && mouseY >= cy + 4 && mouseY < cy + 30) {
                hoveredStack = e.stack();
            }
            int index = i;
            int y0 = Math.max(cy, top), y1 = Math.min(cy + CARD_HEIGHT, height - 8);
            if (y1 > y0) {
                hits.add(new Hit(cx, y0, cx + cardWidth, y1, () -> entry = index));
            }
        }
        g.disableScissor();

        renderGridDetail(g, cat);
    }

    private void renderGridDetail(GuiGraphics g, BuyMenuView.Category cat) {
        int x0 = width - RIGHT;
        g.fill(x0, HEADER, width, height, PANEL);
        g.fill(x0, HEADER, x0 + 1, height, BORDER);
        int left = x0 + 10;
        int right = width - 10;
        BuyMenuView.Entry e = currentEntry();
        if (e == null) return;

        int y = HEADER + 8;
        drawSmall(g, upper(cat.name()), left, y, MUTED);
        y += 10;
        g.drawString(font, Component.literal(trim(entryName(e), right - left)).withStyle(ChatFormatting.BOLD), left, y, TEXT, false);
        y += 14;

        iconBox(g, left, y, right - left, 34, e.stack());
        y += 40;

        y = renderStats(g, e.stats(), left, right, y);
        if (!e.stats().isEmpty() && !e.info().isEmpty()) {
            g.fill(left, y + 1, right, y + 2, BORDER);
            y += 6;
        }
        for (Map.Entry<String, String> row : e.info().entrySet()) {
            String value = text(row.getValue());
            g.drawString(font, trim(text(row.getKey()), right - left - font.width(value) - 6), left, y, MUTED, false);
            g.drawString(font, value, right - font.width(value), y, TEXT, false);
            y += 11;
        }

        int total = e.price();
        boolean canBuy = e.available() && total <= balance();
        String label = !e.available() ? tr("buymenu.unrealzaruba.unavailable")
                : total > balance() ? tr("buymenu.unrealzaruba.no_funds")
                : tr("buymenu.unrealzaruba.buy", Money.format(total));
        renderBuyFooter(g, left, right, tr("buymenu.unrealzaruba.price"), total, label, canBuy,
                () -> buy(group, category, List.of(entry)));
    }

    // ---------- sets tab ----------

    private void renderSets(GuiGraphics g, int mouseX, int mouseY) {
        List<SetRef> sets = sets();
        if (sets.isEmpty()) return;
        set = Math.min(set, sets.size() - 1);

        int left = 12;
        int right = width - RIGHT - 12;
        BuyMenuView.Tab t = currentTab();
        String heading = t.groups().size() == 1 ? upper(t.groups().get(0).name()) : upper(t.name());
        g.drawString(font, heading, left, HEADER + 8, TEXT, false);
        g.drawString(font, trim(tr("buymenu.unrealzaruba.sets_hint"), right - left), left, HEADER + 20, MUTED, false);

        int top = HEADER + 34;
        int cols = right - left >= 300 ? 2 : 1;
        int cardWidth = (right - left - GAP * (cols - 1)) / cols;
        int rows = (sets.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (SET_CARD_HEIGHT + GAP) - GAP - (height - 8 - top));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        g.enableScissor(left, top, right, height - 8);
        for (int i = 0; i < sets.size(); i++) {
            BuyMenuView.Category s = sets.get(i).data();
            int cx = left + (i % cols) * (cardWidth + GAP);
            int cy = top + (i / cols) * (SET_CARD_HEIGHT + GAP) - scroll;
            if (cy + SET_CARD_HEIGHT < top || cy > height) continue;

            boolean on = i == set;
            g.fill(cx, cy, cx + cardWidth, cy + SET_CARD_HEIGHT, on ? CARD_SELECTED : CARD);
            g.renderOutline(cx, cy, cardWidth, SET_CARD_HEIGHT, on ? ACCENT : BORDER);
            ItemStack icon = setIcon(s);
            iconBox(g, cx + 5, cy + 5, 40, SET_CARD_HEIGHT - 10, icon);

            int tx = cx + 52;
            int tw = cardWidth - 58;
            int price = s.entries().stream().mapToInt(BuyMenuView.Entry::price).sum();
            String priceText = Money.format(price);
            g.drawString(font, priceText, cx + cardWidth - 6 - font.width(priceText), cy + 7, ACCENT, false);
            g.drawString(font, Component.literal(trim(text(s.name()), tw - font.width(priceText) - 6)).withStyle(ChatFormatting.BOLD),
                    tx, cy + 7, TEXT, false);
            int lineY = cy + 19;
            for (var line : font.split(Component.literal(text(s.note())), (int) (tw / 0.75f)).stream().limit(2).toList()) {
                g.pose().pushPose();
                g.pose().translate(tx, lineY, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                g.drawString(font, line, 0, 0, MUTED, false);
                g.pose().popPose();
                lineY += 8;
            }
            renderStats(g, s.stats(), tx, cx + cardWidth - 6, Math.max(lineY + 2, cy + 38));

            int index = i;
            int y0 = Math.max(cy, top), y1 = Math.min(cy + SET_CARD_HEIGHT, height - 8);
            if (y1 > y0) {
                hits.add(new Hit(cx, y0, cx + cardWidth, y1, () -> selectSet(index)));
            }
        }
        g.disableScissor();

        renderSetDetail(g, sets.get(set), mouseX, mouseY);
    }

    private void renderSetDetail(GuiGraphics g, SetRef ref, int mouseX, int mouseY) {
        int x0 = width - RIGHT;
        g.fill(x0, HEADER, width, height, PANEL);
        g.fill(x0, HEADER, x0 + 1, height, BORDER);
        int left = x0 + 10;
        int right = width - 10;
        BuyMenuView.Category s = ref.data();

        int y = HEADER + 8;
        drawSmall(g, upper(s.hint()), left, y, MUTED);
        y += 10;
        g.drawString(font, Component.literal(trim(text(s.name()), right - left)).withStyle(ChatFormatting.BOLD), left, y, TEXT, false);
        y += 14;
        g.fill(left, y, right, y + 1, BORDER);
        y += 6;
        drawSmall(g, upper(tr("buymenu.unrealzaruba.pieces")), left, y, MUTED);
        y += 10;

        int picked = 0, total = 0;
        boolean allAvailable = true;
        List<Integer> chosen = new ArrayList<>();
        for (int i = 0; i < s.entries().size(); i++) {
            BuyMenuView.Entry piece = s.entries().get(i);
            boolean on = !piecesOff.contains(i);
            if (on) {
                picked++;
                total += piece.price();
                chosen.add(i);
                allAvailable &= piece.available();
            }

            int rowHeight = 22;
            g.fill(left, y, right, y + rowHeight, on ? CARD_SELECTED : CARD);
            g.renderOutline(left, y, right - left, rowHeight, on ? ACCENT : BORDER_LIGHT);
            int box = left + 5;
            int boxY = y + (rowHeight - 9) / 2;
            if (on) {
                g.fill(box, boxY, box + 9, boxY + 9, ACCENT);
                g.drawString(font, "✓", box + 1, boxY + 1, ON_ACCENT, false);
            } else {
                g.renderOutline(box, boxY, 9, 9, MUTED);
            }
            g.renderItem(piece.stack(), box + 13, y + 3);

            String price = Money.format(piece.price());
            g.drawString(font, price, right - 5 - font.width(price), y + 7, ACCENT, false);
            int textX = box + 32;
            int textWidth = right - 10 - font.width(price) - textX;
            drawSmall(g, trim(upper(piece.slot()), (int) (textWidth / 0.75f)), textX, y + 3, MUTED);
            g.drawString(font, trim(entryName(piece), textWidth), textX, y + 11, piece.available() ? TEXT : MUTED, false);

            if (mouseX >= box + 13 && mouseX < box + 29 && mouseY >= y + 3 && mouseY < y + 19) {
                hoveredStack = piece.stack();
            }
            int index = i;
            hits.add(new Hit(left, y, right, y + rowHeight, () -> {
                if (!piecesOff.remove(index)) piecesOff.add(index);
            }));
            y += rowHeight + 3;
        }

        String selected = tr("buymenu.unrealzaruba.pieces_selected");
        String count = picked + " / " + s.entries().size();
        int footerTop = height - 62;
        if (y < footerTop - 10) {
            g.drawString(font, selected, left, footerTop - 10, MUTED, false);
            g.drawString(font, count, right - font.width(count), footerTop - 10, TEXT, false);
        }

        boolean canBuy = picked > 0 && allAvailable && total <= balance();
        String label = picked == 0 ? tr("buymenu.unrealzaruba.select_piece")
                : !allAvailable ? tr("buymenu.unrealzaruba.unavailable")
                : total > balance() ? tr("buymenu.unrealzaruba.no_funds")
                : tr("buymenu.unrealzaruba.buy", Money.format(total));
        renderBuyFooter(g, left, right, tr("buymenu.unrealzaruba.total"), total, label, canBuy,
                () -> buy(ref.group(), ref.category(), chosen));
    }

    private ItemStack setIcon(BuyMenuView.Category s) {
        // The chest piece usually reads best; fall back to whatever the set has
        if (s.entries().size() > 1 && s.entries().get(1).available()) return s.entries().get(1).stack();
        return s.entries().stream().map(BuyMenuView.Entry::stack).filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
    }

    // ---------- shared pieces ----------

    private void renderBuyFooter(GuiGraphics g, int left, int right, String priceLabel, int total, String buttonText,
                                 boolean enabled, Runnable onBuy) {
        int buttonTop = height - 32;
        int priceY = buttonTop - 16;

        if (result != null) {
            int resultY = priceY - 14;
            g.drawString(font, trim(result.getString(), right - left), left, resultY, resultSuccess ? SUCCESS : ERROR, false);
        }

        String price = Money.format(total);
        g.drawString(font, Component.literal(priceLabel).withStyle(ChatFormatting.BOLD), left, priceY, TEXT, false);
        g.drawString(font, Component.literal(price).withStyle(ChatFormatting.BOLD), right - font.width(price) - 1, priceY, TEXT, false);

        g.fill(left, buttonTop, right, buttonTop + 22, enabled ? ACCENT : BORDER);
        String text = upper(buttonText);
        g.drawString(font, Component.literal(trim(text, right - left - 8)).withStyle(ChatFormatting.BOLD),
                (left + right - Math.min(font.width(text), right - left - 8)) / 2, buttonTop + 7, enabled ? ON_ACCENT : MUTED, false);
        if (enabled) {
            hits.add(new Hit(left, buttonTop, right, buttonTop + 22, onBuy));
        }
    }

    /** 0-100 bars with the value on the right. Returns the y below the last bar. */
    private int renderStats(GuiGraphics g, Map<String, Integer> stats, int left, int right, int y) {
        int labelWidth = 0;
        for (String label : stats.keySet()) {
            labelWidth = Math.max(labelWidth, (int) (font.width(text(label)) * 0.75f));
        }
        labelWidth = Math.min(labelWidth, (right - left) / 2);
        for (Map.Entry<String, Integer> stat : stats.entrySet()) {
            int value = Mth.clamp(stat.getValue(), 0, 100);
            drawSmall(g, trim(text(stat.getKey()), (int) (labelWidth / 0.75f)), left, y, MUTED);
            String number = String.valueOf(value);
            int barLeft = left + labelWidth + 5;
            int barRight = right - (int) (font.width("100") * 0.75f) - 5;
            g.fill(barLeft, y + 2, barRight, y + 4, BAR_BG);
            g.fill(barLeft, y + 2, barLeft + (barRight - barLeft) * value / 100, y + 4, TEXT);
            drawSmall(g, number, right - (int) (font.width(number) * 0.75f), y, TEXT);
            y += 9;
        }
        return y;
    }

    private void iconBox(GuiGraphics g, int x, int y, int w, int h, ItemStack stack) {
        g.fill(x, y, x + w, y + h, ICON_BG);
        g.renderOutline(x, y, w, h, ICON_EDGE);
        if (stack.isEmpty()) {
            g.drawCenteredString(font, "?", x + w / 2, y + (h - 8) / 2, MUTED);
            return;
        }
        float scale = Math.min(2.0f, (h - 4) / 16f);
        g.pose().pushPose();
        g.pose().translate(x + w / 2f - 8 * scale, y + h / 2f - 8 * scale, 0);
        g.pose().scale(scale, scale, 1);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    private void drawSmall(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private String entryName(BuyMenuView.Entry e) {
        if (!e.name().isEmpty()) return text(e.name());
        return e.stack().isEmpty() ? "?" : e.stack().getHoverName().getString();
    }

    /** Config strings may be lang keys or plain text; an unknown key renders as itself. */
    private static String text(String value) {
        return value.isEmpty() ? "" : Component.translatable(value).getString();
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String upper(String value) {
        return text(value).toUpperCase(Locale.ROOT);
    }

    private String trim(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
    }
}
