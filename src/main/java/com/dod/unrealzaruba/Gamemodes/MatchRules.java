package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.Title.TitleMessage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The rules of the match are on the TAB panel, not in the chat. A player who comes to pick a team is asked, loudly and once, to open it:
 * a big title and a single line.
 */
public final class MatchRules {
    private MatchRules() {}

    public static void tell(ServerPlayer player) {
        TitleMessage.showTitle(player,
                Component.translatable("hud.unrealzaruba.rules.hint_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("hud.unrealzaruba.rules.hint_subtitle"), 100);
        player.sendSystemMessage(Component.translatable("hud.unrealzaruba.rules.hint").withStyle(ChatFormatting.GOLD));
    }
}
