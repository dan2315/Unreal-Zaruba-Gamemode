package com.dod.unrealzaruba.utils;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public class NBT {

    public static void addItemStackTag(ItemStack itemStack, String nameTag, Integer value) {
        if (itemStack != null) {
            CustomData.update(DataComponents.CUSTOM_DATA, itemStack, nbt -> nbt.putInt(nameTag, value));
        }
    }

    public static void addEntityTag(Entity entity, String nameTag, Integer value) {
        if (entity != null) {
            CompoundTag nbt = entity.getPersistentData();
            nbt.putInt(nameTag, value);
        }
    }

    public static int readItemStackTag(ItemStack itemStack, String nameTag) {
        if (itemStack != null && itemStack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag nbt = itemStack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (nbt.contains(nameTag)) {
                return nbt.getInt(nameTag);
            }
        }
        return 0;
    }

    public static int readEntityTag(Entity entity, String nameTag) {
        if (entity != null) {
            CompoundTag nbt = entity.getPersistentData();
            if (nbt.contains(nameTag)) {
                return nbt.getInt(nameTag);
            }
        }
        return 0;
    }
}
