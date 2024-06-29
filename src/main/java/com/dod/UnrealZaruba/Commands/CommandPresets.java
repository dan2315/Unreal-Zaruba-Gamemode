package com.dod.UnrealZaruba.Commands;

import net.minecraft.server.MinecraftServer;

public class CommandPresets {
    public static void executeGiveCommandSilent(MinecraftServer server, String playerName, String itemCommand) {
        server.getCommands().performCommand(
                server.createCommandSourceStack().withSuppressedOutput().withPermission(4),
                "give " + playerName + " " + itemCommand);
    }
}
