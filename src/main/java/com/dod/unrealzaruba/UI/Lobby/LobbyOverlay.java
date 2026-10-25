package com.dod.unrealzaruba.UI.Lobby;

import com.dod.unrealzaruba.Events.KeyBindings;
import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.ZarubaOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The lobby on the screen while voting is on: a small panel in the top right corner with the voted mode, how many
 * players are ready and, while too few are online, the minimum for a match; and a bar above the hotbar once the player
 * is ready themselves.
 */
public class LobbyOverlay implements ZarubaOverlay {
    public static final LobbyOverlay INSTANCE = new LobbyOverlay();

    private static final int WIDTH = 150;

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!LobbyClientState.active() || minecraft.screen != null || minecraft.level == null) return;
        // The overlay is for the lobby, not for the map of a match
        if (minecraft.level.dimension().location().getPath().equals("game_dim")) return;
        Font font = com.dod.unrealzaruba.UI.UiFont.get();

        renderPanel(g, font, screenWidth);
        if (LobbyClientState.myReady()) {
            renderReadyBar(g, font, screenWidth, screenHeight);
        }
    }

    private void renderPanel(GuiGraphics g, Font font, int screenWidth) {
        String leader = LobbyClientState.leader();
        String mode = leader == null
                ? Component.translatable("lobby.unrealzaruba.mode_none").getString()
                : modeName(leader);
        String ready = Component.translatable("lobby.unrealzaruba.ready_count", LobbyClientState.ready(), LobbyClientState.players()).getString();
        String keys = Component.translatable("lobby.unrealzaruba.keys",
                KeyBindings.OPEN_GAMEMODE_MENU.getTranslatedKeyMessage().getString(),
                KeyBindings.READY.getTranslatedKeyMessage().getString()).getString();

        // Too few players online for a match: the minimum, under the count, until enough have joined
        boolean tooFew = LobbyClientState.players() < LobbyClientState.minPlayers();
        String minimum = Component.translatable("lobby.unrealzaruba.min_players", LobbyClientState.minPlayers()).getString();

        int height = 12 + 11 + 12 + 12 + 12 + (tooFew ? 12 : 0);
        int x = screenWidth - WIDTH - HudStyle.MARGIN;
        int y = HudStyle.MARGIN;
        g.fill(x, y, x + WIDTH, y + height, Brand.alpha(Brand.WINDOW, 0xD9));
        g.renderOutline(x, y, WIDTH, height, Brand.BORDER);
        Brand.gradient(g, x, y, WIDTH, 2, Brand.MINT_DIM, Brand.CYAN_DIM, Brand.BLUE);

        int left = x + 8;
        int cy = y + 8;
        HudStyle.text(g, font, Component.translatable("lobby.unrealzaruba.title").getString().toUpperCase(), left, cy, Brand.MINT, 0.8f);
        cy += 11;
        g.drawString(font, HudStyle.trim(font, mode, WIDTH - 16), left, cy, Brand.TEXT, false);
        cy += 12;
        g.drawString(font, ready, left, cy, Brand.TEXT_DIM, false);
        cy += 12;
        if (tooFew) {
            g.drawString(font, HudStyle.trim(font, minimum, WIDTH - 16), left, cy, Brand.WARN, false);
            cy += 12;
        }
        HudStyle.text(g, font, HudStyle.trim(font, keys, (int) ((WIDTH - 16) / 0.8f)), left, cy + 1, Brand.TEXT_MUTE, 0.8f);
    }

    private void renderReadyBar(GuiGraphics g, Font font, int screenWidth, int screenHeight) {
        String text = Component.translatable("lobby.unrealzaruba.you_ready", LobbyClientState.ready(), LobbyClientState.players()).getString();
        String hint = Component.translatable("lobby.unrealzaruba.cancel_hint", KeyBindings.READY.getTranslatedKeyMessage().getString()).getString();
        int width = Math.max(font.width(text), (int) (font.width(hint) * 0.8f)) + 28;
        int height = 28;
        int x = (screenWidth - width) / 2;
        int y = screenHeight - 58 - height;
        g.fill(x, y, x + width, y + height, Brand.alpha(Brand.WINDOW, 0xD9));
        g.renderOutline(x, y, width, height, Brand.MINT);
        // A mint tick in front of the text
        g.fill(x + 8, y + 12, x + 10, y + 16, Brand.MINT);
        g.fill(x + 10, y + 14, x + 12, y + 18, Brand.MINT);
        g.fill(x + 12, y + 10, x + 14, y + 16, Brand.MINT);
        g.fill(x + 14, y + 8, x + 16, y + 12, Brand.MINT);
        g.drawString(font, text, x + 22, y + 5, Brand.TEXT, false);
        HudStyle.text(g, font, hint, x + 22, y + 17, Brand.TEXT_MUTE, 0.8f);
    }

    private static String modeName(String id) {
        String key = "lobby.unrealzaruba.mode." + id;
        Component name = Component.translatable(key);
        return name.getString().equals(key) ? id : name.getString();
    }
}
