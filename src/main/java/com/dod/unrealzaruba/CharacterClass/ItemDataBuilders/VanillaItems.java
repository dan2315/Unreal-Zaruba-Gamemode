package com.dod.unrealzaruba.CharacterClass.ItemDataBuilders;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public class VanillaItems {

    /**
     * Enchantments are data-driven since 1.21, so the curse is applied at equip time
     * via {@link #applyBindingCurse} when registry access is available.
     */
    public static ItemStack getStickyPumpkin() {
        return new ItemStack(Items.CARVED_PUMPKIN);
    }

    public static void applyBindingCurse(ItemStack stack, RegistryAccess registryAccess) {
        stack.enchant(registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE), 1);
    }
}
