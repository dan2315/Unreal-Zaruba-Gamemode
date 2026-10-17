package com.dod.unrealzaruba.Schematics;

import com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundPurchaseSessionPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Client side of the joint purchase packets. Kept out of the packet classes: the dedicated server verifies those, and
 * verifying code that hands a screen to Minecraft.setScreen loads client-only classes.
 */
public final class PurchaseClientHandlers {
    private PurchaseClientHandlers() {}

    public static void onSession(ClientboundPurchaseSessionPacket packet) {
        PurchaseClientState.set(packet);
    }

    public static void openSession(BlockPos pos) {
        if (PurchaseClientState.get(pos) != null) {
            Minecraft.getInstance().setScreen(new PurchaseSessionScreen(pos));
        }
    }
}
