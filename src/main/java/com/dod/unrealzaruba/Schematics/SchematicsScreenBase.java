package com.dod.unrealzaruba.Schematics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Look and plumbing shared by the schematics screens: the buy menu palette, click areas, buttons, status line. */
abstract class SchematicsScreenBase extends Screen {
    protected static final int BG = 0xF00D1014;
    protected static final int PANEL = 0xFF12161B;
    protected static final int SIDE = 0xFF10141A;
    protected static final int BORDER = 0xFF262D36;
    protected static final int CARD = 0xFF151A20;
    protected static final int CARD_SELECTED = 0xFF1F2227;
    protected static final int TAB_ACTIVE = 0xFF1A2027;
    protected static final int IMAGE_BG = 0xFF1A2028;
    protected static final int FIELD = 0xFF0B0E12;
    protected static final int TEXT = 0xFFE8ECEF;
    protected static final int MUTED = 0xFF9AA6B2;
    protected static final int ACCENT = 0xFFF2A33A;
    protected static final int ON_ACCENT = 0xFF14110A;
    protected static final int SUCCESS = 0xFF6FCF7F;
    protected static final int ERROR = 0xFFE5676B;

    protected static final int HEADER = 26;
    private static final int STATUS_TICKS = 100;

    protected record Hit(int x0, int y0, int x1, int y1, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    protected enum Style { PRIMARY, NORMAL, DANGER }

    protected final List<Hit> hits = new ArrayList<>();
    private Component status;
    private boolean statusError;
    private int statusTicks;

    protected SchematicsScreenBase(Component title) {
        super(title);
    }

    /** Draws the screen under the widgets and registers click areas. */
    protected abstract void renderContent(GuiGraphics g, int mouseX, int mouseY);

    protected void showStatus(Component message, boolean error) {
        status = message;
        statusError = error;
        statusTicks = STATUS_TICKS;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (statusTicks > 0 && --statusTicks == 0) {
            status = null;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, BG);
        hits.clear();
        renderContent(g, mouseX, mouseY);
        if (status != null) {
            int w = Math.min(width - 20, font.width(status) + 16);
            int x = (width - w) / 2, y = height - 22;
            g.fill(x, y, x + w, y + 16, PANEL);
            g.renderOutline(x, y, w, 16, statusError ? ERROR : SUCCESS);
            g.drawCenteredString(font, Component.literal(trim(status.getString(), w - 12)), width / 2, y + 4, statusError ? ERROR : SUCCESS);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (hit.contains(mouseX, mouseY)) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    setFocused(null);
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected boolean typing() {
        return getFocused() instanceof EditBox || getFocused() instanceof net.minecraft.client.gui.components.MultiLineEditBox;
    }

    // ---------- drawing ----------

    protected void header(GuiGraphics g, Component title) {
        g.fill(0, 0, width, HEADER, PANEL);
        g.fill(0, HEADER - 1, width, HEADER, BORDER);
        g.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), 10, (HEADER - 8) / 2, TEXT, false);
    }

    /** A header tab; returns its width. */
    protected int tab(GuiGraphics g, int x, String label, boolean on, Runnable action) {
        String text = upper(label);
        int w = font.width(text) + 20;
        if (on) {
            g.fill(x, 3, x + w, HEADER - 1, TAB_ACTIVE);
            g.fill(x, HEADER - 3, x + w, HEADER - 1, ACCENT);
        }
        g.drawString(font, text, x + 10, (HEADER - 8) / 2, on ? TEXT : MUTED, false);
        hits.add(new Hit(x, 0, x + w, HEADER, action));
        return w;
    }

    protected void button(GuiGraphics g, int x, int y, int w, int h, String label, Style style, boolean enabled, Runnable action) {
        int bg = !enabled ? BORDER : switch (style) {
            case PRIMARY -> ACCENT;
            case DANGER -> 0xFF5A2326;
            case NORMAL -> CARD_SELECTED;
        };
        int fg = !enabled ? MUTED : style == Style.PRIMARY ? ON_ACCENT : style == Style.DANGER ? 0xFFFFD6D7 : TEXT;
        g.fill(x, y, x + w, y + h, bg);
        if (enabled && style == Style.NORMAL) g.renderOutline(x, y, w, h, BORDER);
        String text = trim(upper(label), w - 8);
        g.drawString(font, Component.literal(text).withStyle(ChatFormatting.BOLD), x + (w - font.width(text)) / 2, y + (h - 8) / 2 + 1, fg, false);
        if (enabled) hits.add(new Hit(x, y, x + w, y + h, action));
    }

    /** A small toggle chip; returns its width. */
    protected int chip(GuiGraphics g, int x, int y, String label, boolean on, Runnable action) {
        String text = upper(label);
        int w = (int) (font.width(text) * 0.75f) + 12;
        g.fill(x, y, x + w, y + 14, on ? CARD_SELECTED : CARD);
        g.renderOutline(x, y, w, 14, on ? ACCENT : BORDER);
        small(g, text, x + 6, y + 4, on ? ACCENT : MUTED);
        hits.add(new Hit(x, y, x + w, y + 14, action));
        return w;
    }

    /** Background for an EditBox placed at (x+4, y+4). */
    protected void field(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
        g.fill(x, y, x + w, y + h, FIELD);
        g.renderOutline(x, y, w, h, focused ? ACCENT : BORDER);
    }

    protected void label(GuiGraphics g, String key, int x, int y) {
        small(g, upper(tr(key)), x, y, MUTED);
    }

    protected void image(GuiGraphics g, SchematicImages.Texture texture, int x, int y, int w, int h, String placeholder) {
        g.fill(x, y, x + w, y + h, IMAGE_BG);
        if (texture != null) {
            SchematicImages.drawCover(g, texture, x, y, w, h);
        } else if (placeholder != null) {
            String text = trim(placeholder, w - 6);
            small(g, text, x + (w - (int) (font.width(text) * 0.75f)) / 2, y + h / 2 - 3, MUTED);
        }
    }

    protected void small(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    protected String trim(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
    }

    protected static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    protected static String upper(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    protected static Component error(String message) {
        return Component.translatable("schematics.unrealzaruba.error", message);
    }
}
