package com.dod.unrealzaruba.Gamemodes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** A few lines about how the match works, said to a player when they come to pick a team. */
public final class MatchRules {
    private static final int LINES = 5;

    private MatchRules() {}

    public static void tell(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("hud.unrealzaruba.rules.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (int i = 1; i <= LINES; i++) {
            player.sendSystemMessage(Component.literal("\u2022 ").withStyle(ChatFormatting.GRAY).append(Component.translatable("hud.unrealzaruba.rules.line" + i)));
        }
    }
}
