package com.dod.UnrealZaruba.Commands.Arguments;

import net.minecraft.ChatFormatting;

public enum TeamColor {
    UNDEFINED,
    RED,
    BLUE,
    PURPLE,
    YELLOW;

    public static String getColorCodeForTeam(TeamColor teamColor) {
        return switch (teamColor) {
            case RED -> ChatFormatting.RED.toString();
            case BLUE -> ChatFormatting.BLUE.toString();
            case PURPLE -> ChatFormatting.DARK_PURPLE.toString();
            case YELLOW -> ChatFormatting.YELLOW.toString();
            default -> ChatFormatting.WHITE.toString();
        };
    }
}



