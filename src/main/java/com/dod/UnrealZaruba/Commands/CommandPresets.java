package com.dod.UnrealZaruba.Commands;

import net.minecraft.server.MinecraftServer;

public class CommandPresets {
    public static void executeGiveCommandSilent(MinecraftServer server, String playerName, String itemCommand) {
        server.getCommands().performCommand(
                server.createCommandSourceStack().withSuppressedOutput().withPermission(4),
                "give " + playerName + " " + itemCommand);
    }

    public static void executeEquipArmorCommandSilent(MinecraftServer server, String playerName, String armorSlot,
            String armorItem) {
        String command = String.format("item replace entity %s armor.%s with %s", playerName, armorSlot, armorItem);
        server.getCommands().performCommand(
                server.createCommandSourceStack().withSuppressedOutput().withPermission(4),
                command);
    }
}
