package com.dod.UnrealZaruba.Commands.Arguments;

import net.minecraft.ChatFormatting;

public enum TeamColor {
    UNDEFINED,
    RED,
    BLUE,
    PURPLE,
    YELLOW;


    public static String getColorCodeForTeam(TeamColor teamColor) {
    switch (teamColor) {
        case RED:
            return ChatFormatting.RED.toString();
        case BLUE:
            return ChatFormatting.BLUE.toString();
        case PURPLE:
            return ChatFormatting.DARK_PURPLE.toString();
        case YELLOW:
            return ChatFormatting.YELLOW.toString();
        default:
            return ChatFormatting.WHITE.toString();
    }
}
}



