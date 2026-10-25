package com.dod.unrealzaruba.UI.BuyMenu;

import com.dod.unrealzaruba.Economy.BuyMenuView;
import com.dod.unrealzaruba.UI.MoneyOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client side of the economy packets. Kept out of the packet classes: the dedicated server verifies those, and
 * verifying code that hands a screen to Minecraft.setScreen loads client-only classes.
 */
public final class ClientEconomyHandlers {
    private ClientEconomyHandlers() {}

    public static void setMoney(boolean visible, int balance) {
        MoneyOverlay.INSTANCE.set(visible, balance);
    }

    public static void setZoneHints(boolean shown, boolean shopEnabled, boolean shopOpen, boolean storageOpen) {
        com.dod.unrealzaruba.UI.ZoneHintsOverlay.INSTANCE.set(shown, shopEnabled, shopOpen, storageOpen);
    }

    public static void openBuyMenu(BuyMenuView view) {
        if (Minecraft.getInstance().screen instanceof BuyMenuScreen open) {
            open.replaceView(view);
            return;
        }
        Minecraft.getInstance().setScreen(new BuyMenuScreen(view));
    }

    public static void showBuyResult(boolean success, Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BuyMenuScreen screen) {
            screen.showResult(success, message);
        } else if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }
}
