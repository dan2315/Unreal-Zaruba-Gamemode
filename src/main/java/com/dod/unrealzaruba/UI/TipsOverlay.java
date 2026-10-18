package com.dod.unrealzaruba.UI;

import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.Events.KeyBindings;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * On the right while TAB is held, next to the objectives panel: what the keys of the mode do (read from the player's
 * own key settings) and a few tips about the game.
 */
public class TipsOverlay implements ZarubaOverlay {
    public static final TipsOverlay INSTANCE = new TipsOverlay();

    private static final int WIDTH = 190;
    private static final int PAD = 8;
    private static final int TOP = 48;
    private static final int TIP_COUNT = 5;

    @Override
    public void render(Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.screen != null || !minecraft.options.keyPlayerList.isDown()) return;
        Font font = UiFont.get();
        int inner = WIDTH - 2 * PAD;

        List<KeyMapping> keys = new ArrayList<>();
        // The map comes first: it is the key a player looks for most. It is Xaero's own key, so it is looked up by name
        for (KeyMapping key : minecraft.options.keyMappings) {
            if (key.getName().equals("gui.xaero_open_map")) keys.add(key);
        }
        keys.addAll(List.of(KeyBindings.OPEN_BUY_MENU, KeyBindings.OPEN_SCHEMATICS,
                KeyBindings.OPEN_TEAM_STORAGE, KeyBindings.OPEN_GAMEMODE_MENU, KeyBindings.READY));
        List<FormattedCharSequence> tips = new ArrayList<>();
        for (int i = 1; i <= TIP_COUNT; i++) {
            tips.addAll(font.split(Component.literal("• ").append(Component.translatable("hud.unrealzaruba.rules.line" + i)), inner));
        }

        int line = font.lineHeight + 3;
        int height = PAD + line + keys.size() * line + 6 + line + tips.size() * (font.lineHeight + 1) + PAD;
        int x = screenWidth - WIDTH - HudStyle.MARGIN;
        int y = Math.max(HudStyle.MARGIN, Math.min(TOP, screenHeight - height - HudStyle.MARGIN));
        HudStyle.panel(g, x, y, WIDTH, height);

        int left = x + PAD;
        int right = x + WIDTH - PAD;
        int cy = y + PAD;
        g.drawString(font, Component.translatable("hud.unrealzaruba.tips.controls").getString().toUpperCase(), left, cy, HudStyle.ACCENT, false);
        cy += line;
        for (KeyMapping key : keys) {
            String name = englishKeyName(key);
            int nameWidth = font.width(name);
            g.fill(right - nameWidth - 6, cy - 2, right, cy + font.lineHeight + 1, HudStyle.BAR_BG);
            g.drawString(font, name, right - nameWidth - 3, cy, HudStyle.TEXT, false);
            String label = Component.translatable(key.getName() + ".hint").getString();
            g.drawString(font, HudStyle.trim(font, label, right - nameWidth - 12 - left), left, cy, HudStyle.MUTED, false);
            cy += line;
        }

        cy += 2;
        g.fill(left, cy, right, cy + 1, HudStyle.DIVIDER);
        cy += 5;
        g.drawString(font, Component.translatable("hud.unrealzaruba.rules.title").getString().toUpperCase(), left, cy, HudStyle.ACCENT, false);
        cy += line;
        for (FormattedCharSequence tip : tips) {
            HudStyle.text(g, font, tip, left, cy, HudStyle.MUTED);
            cy += font.lineHeight + 1;
        }
    }

    /**
     * The name of the key as it is written in English whatever the language of the game is (the game would translate the names of the
     * keys): CAPS LOCK, LEFT BRACKET, MOUSE LEFT, with the modifier in front (SHIFT+B).
     */
    private static String englishKeyName(KeyMapping mapping) {
        if (mapping.isUnbound()) return "-";
        String name = mapping.getKey().getName();
        String rest = name.startsWith("key.keyboard.") ? name.substring("key.keyboard.".length())
                : name.startsWith("key.mouse.") ? "mouse " + name.substring("key.mouse.".length())
                : name.startsWith("key.") ? name.substring(4) : name;
        rest = rest.replace('.', ' ').toUpperCase(java.util.Locale.ROOT);
        var modifier = mapping.getKeyModifier();
        return modifier == net.neoforged.neoforge.client.settings.KeyModifier.NONE ? rest : modifier.name() + "+" + rest;
    }
}
