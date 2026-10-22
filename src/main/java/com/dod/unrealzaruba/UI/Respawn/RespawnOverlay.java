package com.dod.unrealzaruba.UI.Respawn;

import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsView;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UI.ZarubaOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** While dead with the map closed: countdown, the chosen respawn point, and a reminder to open the map to change it. */
public class RespawnOverlay implements ZarubaOverlay {
    public static final RespawnOverlay INSTANCE = new RespawnOverlay();

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        if (!RespawnClientState.isDead() || Minecraft.getInstance().screen != null) return;
        Font font = com.dod.unrealzaruba.UI.UiFont.get();

        boolean choosing = RespawnClientState.isChoosing();
        String countdown = choosing ? Component.translatable("hud.unrealzaruba.respawn.choose_now").getString().toUpperCase()
                : Component.translatable("hud.unrealzaruba.respawn.in", Math.max(0, RespawnClientState.secondsLeft())).getString().toUpperCase();
        // During the flyover there is only the countdown; once it is over the map has to be opened to pick a point
        String point = choosing ? Component.translatable("hud.unrealzaruba.respawn.hint").getString() : "";
        String hint = choosing ? Component.translatable("hud.unrealzaruba.respawn.open_map").getString() : "";

        int width = Math.max(font.width(countdown), Math.max(font.width(point), font.width(hint))) + 16;
        int height = (choosing ? 3 : 1) * (font.lineHeight + 3) + 9;
        int x = (screenWidth - width) / 2;
        int y = screenHeight / 3;
        HudStyle.panel(g, x, y, width, height);
        int lineY = y + 6;
        g.drawCenteredString(font, countdown, screenWidth / 2, lineY, HudStyle.ACCENT);
        if (!choosing) return;
        lineY += font.lineHeight + 3;
        g.drawCenteredString(font, point, screenWidth / 2, lineY, HudStyle.TEXT);
        lineY += font.lineHeight + 3;
        g.drawCenteredString(font, hint, screenWidth / 2, lineY, HudStyle.MUTED);
    }

    private static String describe(RespawnPointsView.Entry entry) {
        return RespawnClientState.describe(entry);
    }
}
