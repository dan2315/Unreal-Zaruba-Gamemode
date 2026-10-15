package com.dod.unrealzaruba.Economy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.dod.unrealzaruba.UnrealZaruba;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Asks TACZ which attachments and ammo suit the guns a player carries, for greying out the rest in the buy menu. */
final class TaczCompat {
    private TaczCompat() {}

    /** True for anything that isn't an attachment or ammo, and for everything while the player carries no gun. */
    static Predicate<ItemStack> fitsGunsOf(ServerPlayer player) {
        List<ItemStack> guns = new ArrayList<>();
        try {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && IGun.getIGunOrNull(stack) != null) guns.add(stack);
            }
        } catch (RuntimeException | LinkageError e) {
            UnrealZaruba.LOGGER.warn("[BuyMenu] TACZ gun lookup failed: {}", e.toString());
            return stack -> true;
        }
        if (guns.isEmpty()) return stack -> true;
        return stack -> {
            try {
                IAttachment attachment = IAttachment.getIAttachmentOrNull(stack);
                if (attachment != null) {
                    return guns.stream().anyMatch(gun -> IGun.getIGunOrNull(gun).allowAttachment(gun, stack));
                }
                IAmmo ammo = IAmmo.getIAmmoOrNull(stack);
                if (ammo != null) {
                    return guns.stream().anyMatch(gun -> ammo.isAmmoOfGun(gun, stack));
                }
            } catch (RuntimeException | LinkageError e) {
                return true;
            }
            return true;
        };
    }
}
