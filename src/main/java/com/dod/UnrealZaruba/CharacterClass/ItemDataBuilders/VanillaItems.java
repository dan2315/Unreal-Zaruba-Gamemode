package com.dod.UnrealZaruba.CharacterClass.ItemDataBuilders;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class VanillaItems {

    public static ItemStack getStickyPumpkin() {
        ItemStack cursedPumpkin = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft:carved_pumpkin")));
        CompoundTag enchTag = new CompoundTag();
        ListTag enchList = new ListTag();
        CompoundTag bindingEnch = new CompoundTag();
        bindingEnch.putString("id", "minecraft:binding_curse");
        bindingEnch.putInt("lvl", 1);
        enchList.add(bindingEnch);
        enchTag.put("Enchantments", enchList);
        cursedPumpkin.setTag(enchTag);
        return cursedPumpkin;
    }
}
