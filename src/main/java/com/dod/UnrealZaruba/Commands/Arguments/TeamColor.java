package com.dod.UnrealZaruba.Commands.Arguments;

import net.minecraft.ChatFormatting;

public enum TeamColor {
    UNDEFINED(ChatFormatting.WHITE, "Undefined Team"),
    RED(ChatFormatting.RED, "Red Team"),
    BLUE(ChatFormatting.BLUE, "Blue Team"),
    PURPLE(ChatFormatting.DARK_PURPLE, "Purple Team"),
    YELLOW(ChatFormatting.YELLOW, "Yellow Team");

    private final ChatFormatting chatFormatting;
    private final String displayName;

    TeamColor(ChatFormatting chatFormatting, String displayName) {
        this.chatFormatting = chatFormatting;
        this.displayName = displayName;
    }

    public ChatFormatting getChatFormatting() {
        return chatFormatting;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return this.toString();
    }
}



