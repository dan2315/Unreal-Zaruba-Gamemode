package com.dod.UnrealZaruba.Utils;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;

public class NBT {
    public static void addItemStackTag(ItemStack itemStack, String nameTag, Integer value) {
        if (itemStack != null) {
            CompoundTag nbt = itemStack.getOrCreateTag();
            nbt.putInt(nameTag, value);
            itemStack.setTag(nbt);
        }
    }

    public static void addEntityTag(Entity entity, String nameTag, Integer value) {
        if (entity != null) {
            CompoundTag nbt = entity.getPersistentData();
            nbt.putInt(nameTag, value);
        }
    }

    public static int readItemStackTag(ItemStack itemStack, String nameTag) {
        if (itemStack != null && itemStack.hasTag()) {
            CompoundTag nbt = itemStack.getTag();
            if (nbt != null && nbt.contains(nameTag)) {
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
