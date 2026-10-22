package com.dod.unrealzaruba.Schematics;

import java.util.List;

import com.dod.unrealzaruba.NetworkPackets.Schematics.GiveToolGunPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * "How to upload your schematic": the steps from building a vehicle to publishing it, with a button that puts the Tool Gun in the hand
 * and one that goes on to the upload. The same text is on the website.
 */
public class SchematicGuideScreen extends SchematicsScreenBase {
    private static final int STEPS = 5;
    private static final int MAX_WIDTH = 440;

    private final SchematicsBrowserScreen parent;

    public SchematicGuideScreen(SchematicsBrowserScreen parent) {
        super(Component.translatable("schematics.unrealzaruba.guide.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.font = com.dod.unrealzaruba.UI.UiFont.get();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY) {
        header(g, title);
        String back = tr("schematics.unrealzaruba.guide.back");
        int backWidth = buttonWidth(back);
        button(g, width - 10 - backWidth, 5, backWidth, HEADER - 10, back, Style.NORMAL, true, this::onClose);

        int w = Math.min(MAX_WIDTH, width - 20);
        int x = (width - w) / 2;
        int y = HEADER + 12;
        int textWidth = w - 28;
        for (int i = 1; i <= STEPS; i++) {
            List<FormattedCharSequence> lines = font.split(Component.translatable("schematics.unrealzaruba.guide.step" + i), textWidth);
            int h = lines.size() * (font.lineHeight + 1) + 10;
            g.fill(x, y, x + w, y + h, CARD);
            g.renderOutline(x, y, w, h, BORDER);
            g.fill(x, y, x + 3, y + h, ACCENT);
            g.drawString(font, String.valueOf(i), x + 9, y + 5, ACCENT, false);
            int ly = y + 5;
            for (FormattedCharSequence line : lines) {
                g.drawString(font, line, x + 24, ly, TEXT, false);
                ly += font.lineHeight + 1;
            }
            y += h + 4;
        }

        y += 4;
        int half = (w - 6) / 2;
        // The Tool Gun is for a single-player world only, where blueprints are made; on a server it is not given out
        boolean singleplayer = minecraft.getSingleplayerServer() != null;
        button(g, x, y, half, 20, tr("schematics.unrealzaruba.guide.get_toolgun"), Style.PRIMARY, singleplayer, () -> {
            PacketDistributor.sendToServer(new GiveToolGunPacket(GiveToolGunPacket.TOOL_GUN));
            showStatus(Component.translatable("schematics.unrealzaruba.guide.toolgun_asked"), false);
        });
        button(g, x + half + 6, y, w - half - 6, 20, tr("schematics.unrealzaruba.guide.get_scanner"), Style.PRIMARY, singleplayer, () -> {
            PacketDistributor.sendToServer(new GiveToolGunPacket(GiveToolGunPacket.SCANNER));
            showStatus(Component.translatable("schematics.unrealzaruba.guide.toolgun_asked"), false);
        });
        y += 24;
        button(g, x, y, w, 20, tr("schematics.unrealzaruba.guide.open_upload"), Style.NORMAL, true,
                () -> minecraft.setScreen(new SchematicEditScreen(parent, null)));
        if (!singleplayer) {
            g.drawCenteredString(font, Component.translatable("schematics.unrealzaruba.guide.singleplayer_only"), width / 2, y + 26, MUTED);
        }
    }
}
