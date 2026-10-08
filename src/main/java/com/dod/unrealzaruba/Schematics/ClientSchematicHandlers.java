package com.dod.unrealzaruba.Schematics;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Client side of the schematic spawner packets. Kept out of the packet classes: the dedicated server verifies those,
 * and verifying code that hands a screen to Minecraft.setScreen loads client-only classes.
 */
public final class ClientSchematicHandlers {
    private ClientSchematicHandlers() {}

    public static void openSpawner(BlockPos pos, int price, int cooldownSeconds) {
        Minecraft.getInstance().setScreen(new SchematicsBrowserScreen(new SchematicsBrowserScreen.Spawner(pos, price, cooldownSeconds)));
    }

    public static void onSpawnResult(boolean success, Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SchematicsBrowserScreen browser && browser.onSpawnResult(success, message)) {
            return;
        }
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }
}
