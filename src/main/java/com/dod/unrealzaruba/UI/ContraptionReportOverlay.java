package com.dod.unrealzaruba.UI;

import java.util.List;

import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.Pricing.ContraptionReport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The contraption scanner panel on the right side of the screen: size, price with its parts, the costliest lines and
 * what stops the build from being published. The server refreshes it every second; it stays
 * until the server clears it (or you leave the world).
 */
public class ContraptionReportOverlay implements ZarubaOverlay {
    public static final ContraptionReportOverlay INSTANCE = new ContraptionReportOverlay();

    private static final int WIDTH = 190;
    private static final int GOOD = 0xFF6CC47A;
    private static final int LINES_SHOWN = 5;

    private volatile ContraptionReport report;

    public void set(ContraptionReport report) {
        this.report = report;
    }

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        ContraptionReport report = this.report;
        if (report == null || mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        Font font = com.dod.unrealzaruba.UI.UiFont.get();
        int line = font.lineHeight + 2;
        int inner = WIDTH - 16;

        // Measure first: the panel is as tall as its content
        List<FormattedCharSequence> problems = new java.util.ArrayList<>();
        for (String problem : report.problems()) problems.addAll(font.split(Component.literal(problem), inner));
        int rows = 3; // title, size, price
        if (!report.rulesMissing()) rows += 3 + Math.min(LINES_SHOWN, report.lines().size()) + (report.unpricedCount() > 0 ? 1 : 0);
        int height = 12 + rows * line + (problems.isEmpty() ? 0 : 6 + problems.size() * line);
        if (report.rulesMissing()) height += 2 * line;

        int x = screenWidth - HudStyle.MARGIN - WIDTH;
        int y = Math.max(HudStyle.MARGIN, (screenHeight - height) / 2);
        HudStyle.panel(g, x, y, WIDTH, height);
        int left = x + 8, right = x + WIDTH - 8;
        int cy = y + 6;

        HudStyle.text(g, font, HudStyle.trim(font, report.name().isEmpty() ? tr("contraption.unrealzaruba.unnamed") : report.name(), inner),
                left, cy, HudStyle.ACCENT, 1f);
        cy += line;
        HudStyle.text(g, font, sizeText(report), left, cy, report.allowed() || report.size() == null ? HudStyle.MUTED : HudStyle.RED, 1f);
        cy += line;

        if (report.rulesMissing()) {
            for (FormattedCharSequence text : font.split(Component.translatable("contraption.unrealzaruba.no_rules"), inner)) {
                HudStyle.text(g, font, text, left, cy, HudStyle.RED);
                cy += line;
            }
            return;
        }

        String price = Money.format(report.total());
        int priceColor = report.allowed() ? GOOD : HudStyle.RED;
        HudStyle.text(g, font, price, left, cy, priceColor, 1.6f);
        String verdict = tr(report.allowed() ? "contraption.unrealzaruba.allowed" : "contraption.unrealzaruba.refused");
        HudStyle.text(g, font, verdict, right - font.width(verdict), cy + 2, priceColor, 1f);
        cy += line + 6;

        row(g, font, left, right, cy, tr("contraption.unrealzaruba.blocks"), Money.format(report.blocks()), HudStyle.MUTED);
        cy += line;
        row(g, font, left, right, cy, tr("contraption.unrealzaruba.items"), Money.format(report.items()), HudStyle.MUTED);
        cy += line;
        row(g, font, left, right, cy, tr("contraption.unrealzaruba.fluids"), Money.format(report.fluids()), HudStyle.MUTED);
        cy += line;

        int shown = Math.min(LINES_SHOWN, report.lines().size());
        for (int i = 0; i < shown; i++) {
            ContraptionReport.Line entry = report.lines().get(i);
            String name = HudStyle.trim(font, amount(entry) + " " + shortId(entry.id()), inner - 56);
            row(g, font, left, right, cy, name, Money.format(entry.subtotal()), HudStyle.DIM);
            cy += line;
        }
        if (report.unpricedCount() > 0) {
            HudStyle.text(g, font, Component.translatable("contraption.unrealzaruba.unpriced", report.unpricedCount()),
                    left, cy, HudStyle.DIM, 1f);
            cy += line;
        }

        if (!problems.isEmpty()) {
            cy += 6;
            for (FormattedCharSequence text : problems) {
                HudStyle.text(g, font, text, left, cy, HudStyle.RED);
                cy += line;
            }
        }
    }

    private static void row(GuiGraphics g, Font font, int left, int right, int y, String label, String value, int color) {
        HudStyle.text(g, font, label, left, y, color, 1f);
        HudStyle.text(g, font, value, right - font.width(value), y, HudStyle.TEXT, 1f);
    }

    private static String sizeText(ContraptionReport report) {
        if (report.size() == null) return tr("contraption.unrealzaruba.empty");
        String size = report.size()[0] + "×" + report.size()[1] + "×" + report.size()[2];
        if (report.maxSize() == null) return size;
        return tr("contraption.unrealzaruba.size", size, report.maxSize()[0] + "×" + report.maxSize()[1] + "×" + report.maxSize()[2]);
    }

    private static String amount(ContraptionReport.Line line) {
        if (line.kind() == 2) {
            double buckets = line.count() / 1000.0;
            return (buckets == Math.rint(buckets) ? String.valueOf((long) buckets) : String.format(java.util.Locale.ROOT, "%.1f", buckets)) + " B";
        }
        return line.count() + "x";
    }

    private static String shortId(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }
}
