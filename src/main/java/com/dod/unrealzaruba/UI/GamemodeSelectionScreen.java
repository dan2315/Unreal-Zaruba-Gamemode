package com.dod.unrealzaruba.UI;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.Events.KeyBindings;
import com.dod.unrealzaruba.NetworkPackets.GamemodeVotePacket;
import com.dod.unrealzaruba.NetworkPackets.Lobby.ToggleReadyPacket;
import com.dod.unrealzaruba.UI.Lobby.LobbyClientState;
import com.google.gson.Gson;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Lobby mode selection in the Unreal Frontiers look: a card per mode (click to vote, with the vote count and share)
 * and one primary button that marks the player ready.
 */
@OnlyIn(Dist.CLIENT)
public class GamemodeSelectionScreen extends Screen {
    private static final Gson GSON = new Gson();
    private static final int GAP = 12;
    private static final int PAD = 10;
    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 26;

    private record Hit(int x0, int y0, int x1, int y1, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    public record GamemodeDefinition(String id, String name, String description) {}

    private final List<GamemodeDefinition> gamemodes = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();

    public GamemodeSelectionScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        this.font = UiFont.get();
        gamemodes.clear();
        try {
            var resources = Minecraft.getInstance().getResourceManager()
                    .listResources("gamemodes", path -> path.getPath().endsWith(".json"));
            for (var entry : resources.entrySet()) {
                try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                    GamemodeDefinition definition = GSON.fromJson(reader, GamemodeDefinition.class);
                    if (definition != null) gamemodes.add(definition);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        gamemodes.sort((a, b) -> a.id().compareTo(b.id()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, Brand.alpha(Brand.WINDOW, 0xE8));
        // A faint deep-green glow at the bottom, the brand's depth colour
        g.fillGradient(0, height / 2, width, height, 0x000A1F18, 0x660A1F18);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        hits.clear();

        int contentWidth = Math.min(width - 48, 640);
        int left = (width - contentWidth) / 2;

        // Title
        HudStyle.text(g, font, Component.translatable("lobby.unrealzaruba.mode_selection").getString().toUpperCase(),
                (width - HudStyle.width(font, Component.translatable("lobby.unrealzaruba.mode_selection").getString().toUpperCase(), 1.4f)) / 2,
                24, Brand.TEXT, 1.4f);
        Brand.gradient(g, width / 2 - 24, 42, 48, 2, Brand.MINT, Brand.CYAN, Brand.BLUE);
        String subtitle = Component.translatable("lobby.unrealzaruba.vote_hint").getString();
        g.drawCenteredString(font, subtitle, width / 2, 52, Brand.TEXT_MUTE);

        // Cards
        int top = 74;
        int bottomArea = 104;
        int cardHeight = Math.max(110, Math.min(240, height - top - bottomArea));
        int count = Math.max(1, gamemodes.size());
        int cardWidth = (contentWidth - GAP * (count - 1)) / count;
        int totalVotes = 0;
        for (GamemodeDefinition mode : gamemodes) totalVotes += LobbyClientState.votesFor(mode.id());
        String leader = LobbyClientState.leader();
        for (int i = 0; i < gamemodes.size(); i++) {
            renderCard(g, gamemodes.get(i), left + i * (cardWidth + GAP), top, cardWidth, cardHeight, mouseX, mouseY, totalVotes,
                    gamemodes.get(i).id().equals(leader));
        }

        // Status line and the ready button
        int statusY = height - 84;
        String status = Component.translatable("lobby.unrealzaruba.ready_count", LobbyClientState.ready(), LobbyClientState.players()).getString();
        if (LobbyClientState.players() < LobbyClientState.minPlayers()) {
            status += "  ·  " + Component.translatable("lobby.unrealzaruba.need_players", LobbyClientState.minPlayers()).getString();
        }
        g.drawCenteredString(font, status, width / 2, statusY, Brand.TEXT_DIM);

        renderReadyButton(g, (width - BUTTON_WIDTH) / 2, height - 66, mouseX, mouseY);

        String hint = Component.translatable("lobby.unrealzaruba.close_hint",
                KeyBindings.OPEN_GAMEMODE_MENU.getTranslatedKeyMessage().getString()).getString();
        g.drawCenteredString(font, hint, width / 2, height - 28, Brand.TEXT_MUTE);
    }

    private void renderCard(GuiGraphics g, GamemodeDefinition mode, int x, int y, int w, int h, int mouseX, int mouseY,
                            int totalVotes, boolean leader) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        boolean selected = mode.id().equals(LobbyClientState.myVote());
        g.fill(x, y, x + w, y + h, hover ? Brand.CARD_HI : Brand.CARD);
        g.renderOutline(x, y, w, h, selected || hover ? Brand.MINT : Brand.BORDER);
        if (selected) g.renderOutline(x + 1, y + 1, w - 2, h - 2, Brand.alpha(Brand.MINT, 0x66));

        // Icon tile with the initial of the mode
        int tile = Math.min(56, h / 3);
        int tileX = x + PAD;
        int tileY = y + PAD;
        g.fill(tileX, tileY, tileX + tile, tileY + tile, 0xFF26232C);
        g.renderOutline(tileX, tileY, tile, tile, Brand.DIVIDER);
        String initial = mode.name().isEmpty() ? "?" : mode.name().substring(0, 1).toUpperCase();
        float scale = tile / 14f;
        HudStyle.text(g, font, initial, tileX + (int) ((tile - font.width(initial) * scale) / 2),
                tileY + (int) ((tile - 8 * scale) / 2), Brand.MINT, scale);

        // Tags next to the tile
        int tagX = tileX + tile + 8;
        if (leader) {
            tagX += tag(g, tagX, tileY, Component.translatable("lobby.unrealzaruba.leader").getString(), Brand.WARN) + 4;
        }
        if (selected) {
            tag(g, tagX, tileY, Component.translatable("lobby.unrealzaruba.your_vote").getString(), Brand.MINT);
        }

        // Name and description
        int textY = tileY + tile + 8;
        g.drawString(font, Component.literal(mode.name()).withStyle(net.minecraft.ChatFormatting.BOLD), x + PAD, textY, Brand.TEXT, false);
        textY += font.lineHeight + 4;
        int footerTop = y + h - 34;
        List<FormattedCharSequence> lines = font.split(Component.literal(mode.description()), w - 2 * PAD);
        for (FormattedCharSequence line : lines) {
            if (textY + font.lineHeight > footerTop - 2) break;
            g.drawString(font, line, x + PAD, textY, Brand.TEXT_DIM, false);
            textY += font.lineHeight + 1;
        }

        // Votes and their share
        g.fill(x + PAD, footerTop, x + w - PAD, footerTop + 1, Brand.DIVIDER);
        int votes = LobbyClientState.votesFor(mode.id());
        String votesText = Component.translatable("lobby.unrealzaruba.votes", votes).getString();
        HudStyle.text(g, font, votesText, x + PAD, footerTop + 7, Brand.TEXT_DIM, 0.9f);
        int barLeft = x + PAD;
        int barRight = x + w - PAD;
        int barY = footerTop + 20;
        g.fill(barLeft, barY, barRight, barY + 3, Brand.BAR_TRACK);
        if (totalVotes > 0 && votes > 0) {
            int filled = Math.max(2, Math.round((barRight - barLeft) * (votes / (float) totalVotes)));
            Brand.gradient(g, barLeft, barY, filled, 3, Brand.MINT_DIM, Brand.CYAN_DIM, Brand.BLUE);
        }

        hits.add(new Hit(x, y, x + w, y + h, () -> {
            LobbyClientState.setMyVote(mode.id());
            PacketDistributor.sendToServer(new GamemodeVotePacket(mode.id()));
        }));
    }

    /** A small label chip; returns its width. */
    private int tag(GuiGraphics g, int x, int y, String text, int color) {
        int w = (int) (font.width(text) * 0.8f) + 8;
        g.fill(x, y, x + w, y + 11, Brand.alpha(color, 0x26));
        g.renderOutline(x, y, w, 11, Brand.alpha(color, 0x99));
        HudStyle.text(g, font, text, x + 4, y + 2, color, 0.8f);
        return w;
    }

    private void renderReadyButton(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + BUTTON_WIDTH && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
        String label;
        if (!LobbyClientState.active()) {
            g.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, Brand.DISABLED);
            label = Component.translatable("lobby.unrealzaruba.game_running").getString();
            g.drawCenteredString(font, label, x + BUTTON_WIDTH / 2, y + 9, Brand.TEXT_MUTE);
            return;
        }
        if (LobbyClientState.myReady()) {
            // Already ready: a calm graphite button, the gradient is for the main action only
            g.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, hover ? Brand.BTN_HI : Brand.BTN);
            g.renderOutline(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, hover ? Brand.MINT : Brand.BTN_BORDER);
            label = Component.translatable("lobby.unrealzaruba.cancel_ready").getString().toUpperCase();
            g.drawCenteredString(font, label, x + BUTTON_WIDTH / 2, y + 9, Brand.TEXT);
        } else {
            if (hover) {
                Brand.gradient(g, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Brand.MINT, Brand.CYAN, Brand.BLUE_BRIGHT);
            } else {
                Brand.gradient(g, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Brand.MINT_DIM, Brand.CYAN_DIM, Brand.BLUE);
            }
            label = Component.translatable("lobby.unrealzaruba.ready").getString().toUpperCase();
            g.drawCenteredString(font, Component.literal(label).withStyle(net.minecraft.ChatFormatting.BOLD),
                    x + BUTTON_WIDTH / 2, y + 9, Brand.TEXT);
        }
        hits.add(new Hit(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, () -> PacketDistributor.sendToServer(new ToggleReadyPacket())));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Hit hit : hits) {
                if (hit.contains(mouseX, mouseY)) {
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.OPEN_GAMEMODE_MENU.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
