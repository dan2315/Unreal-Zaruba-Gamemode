package com.dod.unrealzaruba.Economy;

import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

/** Item strings in /give syntax (e.g. {@code tacz:ammo[custom_data={AmmoId:"tacz:9mm"}]}) used by economy configs. */
public final class ItemSpecs {
    private ItemSpecs() {}

    /** The stack, or empty if the string doesn't parse (logged). */
    public static ItemStack parse(HolderLookup.Provider registries, String spec, int count) {
        if (spec == null || spec.isBlank()) {
            return ItemStack.EMPTY;
        }
        try {
            ItemParser.ItemResult result = new ItemParser(registries).parse(new StringReader(spec));
            return new ItemStack(result.item(), Math.max(1, count), result.components());
        } catch (CommandSyntaxException e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Bad item in economy config '{}': {}", spec, e.getMessage());
            return ItemStack.EMPTY;
        }
    }
}
