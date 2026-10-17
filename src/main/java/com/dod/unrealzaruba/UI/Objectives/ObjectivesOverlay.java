package com.dod.unrealzaruba.UI.Objectives;

import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.ZarubaOverlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Objectives panel on the left, under Xaero's minimap, shown while TAB is held (the minimal row is {@link ObjectivesStrip}): a lettered row per capture point (owner badge, name, status,
 * progress bar), destroy targets with their progress, and the goal text below a divider.
 */
public class ObjectivesOverlay implements ZarubaOverlay {
    private static final int WIDTH = 150;
    private static final int PADDING_X = 7;
    private static final int PADDING_Y = 6;
    private static final int ROW_GAP = 6;
    private static final int BADGE = 11;
    private static final int BAR_HEIGHT = 4;
    private static final float SMALL = 0.75f;
    /** Room for Xaero's minimap and its coordinates line in the top left corner. */
    private static final int MINIMAP_SPACE = 124;
    /** How far the panel sits below the minimap. */
    private static final int PANEL_DROP = 16;

    public boolean isVisible = true;
    // Insertion order is the order the server sent them in, which gives capture points stable letters
    private final LinkedHashMap<Byte, HudObjective> objectivesMap = new LinkedHashMap<>();

    public static ObjectivesOverlay INSTANCE = new ObjectivesOverlay();

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        // The full list is for looking at the match state: shown while the player list key (TAB) is held
        if (!isVisible || objectivesMap.isEmpty() || !net.minecraft.client.Minecraft.getInstance().options.keyPlayerList.isDown()) return;
        Font font = com.dod.unrealzaruba.UI.UiFont.get();

        List<HudObjective> rows = new ArrayList<>();
        List<FormattedCharSequence> footer = new ArrayList<>();
        int innerWidth = WIDTH - 2 * PADDING_X;
        for (HudObjective objective : objectivesMap.values()) {
            if (objective instanceof HudStringObjective text) {
                footer.addAll(font.split(Component.literal(text.getText()), innerWidth));
            } else {
                rows.add(objective);
            }
        }

        int rowHeight = BADGE + 3 + BAR_HEIGHT;
        int height = PADDING_Y + font.lineHeight + ROW_GAP + rows.size() * (rowHeight + ROW_GAP);
        if (!footer.isEmpty()) height += 1 + ROW_GAP + footer.size() * (font.lineHeight + 1);
        height += PADDING_Y - ROW_GAP + (footer.isEmpty() ? 0 : ROW_GAP);

        int x = 0; // against the left edge of the screen
        int y = (ModList.get().isLoaded("xaerominimap") ? MINIMAP_SPACE : HudStyle.MARGIN) + PANEL_DROP;
        y = Math.max(HudStyle.MARGIN, Math.min(y, screenHeight - height - HudStyle.MARGIN));
        HudStyle.panel(g, x, y, WIDTH, height);

        int left = x + PADDING_X;
        int right = x + WIDTH - PADDING_X;
        int cy = y + PADDING_Y;
        g.drawString(font, Component.translatable("hud.unrealzaruba.objectives").getString().toUpperCase(), left, cy, HudStyle.ACCENT, false);
        cy += font.lineHeight + ROW_GAP;

        char letter = 'A';
        for (HudObjective objective : rows) {
            if (objective instanceof HudCapturePointObjective point) {
                renderCapturePoint(g, font, point, point.isRespawnOnly() ? '⌂' : letter++, left, right, cy);
            } else if (objective instanceof HudDestroyObjective target) {
                renderDestroyTarget(g, font, target, left, right, cy);
            }
            cy += rowHeight + ROW_GAP;
        }

        if (!footer.isEmpty()) {
            g.fill(left, cy, right, cy + 1, HudStyle.DIVIDER);
            cy += 1 + ROW_GAP;
            for (FormattedCharSequence line : footer) {
                HudStyle.text(g, font, line, left, cy, HudStyle.MUTED);
                cy += font.lineHeight + 1;
            }
        }
    }

    private void renderCapturePoint(GuiGraphics g, Font font, HudCapturePointObjective point, char letter, int left, int right, int y) {
        boolean capturing = point.getCapturedBy() != null && point.getCapturedBy() != point.getOwner() && point.getProgress() > 0;
        boolean held = point.getOwner() != null && !capturing;

        String status;
        int statusColor;
        int barColor;
        float barFill;
        if (capturing) {
            int percent = Math.round(point.getProgress() * 100);
            status = Component.translatable("hud.unrealzaruba.capturing", percent + "%").getString();
            statusColor = barColor = HudStyle.teamColor(point.getCapturedBy());
            barFill = point.getProgress();
        } else if (held) {
            status = Component.translatable("hud.unrealzaruba.team." + point.getOwner().name().toLowerCase()).getString();
            statusColor = barColor = HudStyle.teamColor(point.getOwner());
            barFill = 1;
        } else {
            status = Component.translatable("hud.unrealzaruba.neutral").getString();
            statusColor = HudStyle.DIM;
            barColor = HudStyle.NEUTRAL;
            barFill = 0;
        }

        // Badge in the owner's color with the point's letter
        g.fill(left, y, left + BADGE, y + BADGE, HudStyle.teamColor(point.getOwner()));
        String letterText = String.valueOf(letter);
        g.drawString(font, letterText, left + (BADGE - font.width(letterText)) / 2 + 1, y + 2, HudStyle.ON_COLOR, false);

        int statusWidth = HudStyle.width(font, status, SMALL);
        int textY = y + (BADGE - 8) / 2 + 1;
        HudStyle.text(g, font, status, right - statusWidth, textY + 1, statusColor, SMALL);
        int nameLeft = left + BADGE + 4;
        g.drawString(font, HudStyle.trim(font, point.GetName(), right - statusWidth - 4 - nameLeft), nameLeft, textY, HudStyle.TEXT, false);

        bar(g, left, right, y + BADGE + 3, barFill, barColor);
    }

    private void renderDestroyTarget(GuiGraphics g, Font font, HudDestroyObjective target, int left, int right, int y) {
        String percent = Math.round(target.getProgress() * 100) + "%";
        int percentWidth = HudStyle.width(font, percent, SMALL);
        int textY = y + (BADGE - 8) / 2 + 1;
        HudStyle.text(g, font, percent, right - percentWidth, textY + 1, HudStyle.ACCENT, SMALL);
        g.drawString(font, HudStyle.trim(font, target.GetName(), right - percentWidth - 4 - left), left, textY, HudStyle.TEXT, false);
        bar(g, left, right, y + BADGE + 3, target.getProgress(), HudStyle.ACCENT);
    }

    private void bar(GuiGraphics g, int left, int right, int y, float fill, int color) {
        g.fill(left, y, right, y + BAR_HEIGHT, HudStyle.BAR_BG);
        int filled = Math.round((right - left) * Math.max(0, Math.min(1, fill)));
        if (filled > 0) {
            g.fill(left, y, left + filled, y + BAR_HEIGHT, color);
        }
    }

    public java.util.Collection<HudObjective> objectives() {
        return objectivesMap.values();
    }

    /** Letter shown for a capture point (A, B, ...), or null if it isn't one. Matches the minimap waypoints. */
    public String letterFor(byte runtimeId) {
        char letter = 'A';
        for (HudObjective objective : objectivesMap.values()) {
            if (objective instanceof HudCapturePointObjective point && !point.isRespawnOnly()) {
                if (objective.GetRuntimeId() == runtimeId) return String.valueOf(letter);
                letter++;
            }
        }
        return null;
    }

    public void AddObjective(HudObjective objective) {
        objectivesMap.put(objective.GetRuntimeId(), objective);
    }

    public void SetObjectives(List<HudObjective> objectives) {
        for (var objective : objectives) {
            objectivesMap.put(objective.GetRuntimeId(), objective);
        }
    }

    public void UpdateObjectives(List<HudObjectiveUpdate> updates) {
        for (var update : updates) {
            if (objectivesMap.containsKey(update.getRuntimeId())) {
                var hudObjective = objectivesMap.get(update.getRuntimeId());
                hudObjective.Update(update.getProgress());
            }
        }
    }

    public void Clear() {
        objectivesMap.clear();
    }

    public void RemoveById(ArrayList<Byte> runtimeIds) {
        for (var id : runtimeIds) {
            objectivesMap.remove(id);
        }
    }
}
