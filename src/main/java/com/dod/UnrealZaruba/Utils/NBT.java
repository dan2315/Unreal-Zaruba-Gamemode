package com.dod.UnrealZaruba.Utils;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;

/**
 * NBT tags control
 */
public class NBT {
    /**
     * Add item stack tag.
     *
     * @param itemStack the item stack
     * @param nameTag   the name tag
     * @param value     the value
     */
    public static void addItemStackTag(ItemStack itemStack, String nameTag, Integer value) {
        if (itemStack != null) {
            CompoundTag nbt = itemStack.getOrCreateTag();
            nbt.putInt(nameTag, value);
            itemStack.setTag(nbt);
        }
    }

    /**
     * Add entity tag.
     *
     * @param entity  the entity
     * @param nameTag the name tag
     * @param value   the value
     */
    public static void addEntityTag(Entity entity, String nameTag, Integer value) {
        if (entity != null) {
            CompoundTag nbt = entity.getPersistentData();
            nbt.putInt(nameTag, value);
        }
    }

    /**
     * Read item stack tag int.
     *
     * @param itemStack the item stack
     * @param nameTag   the name tag
     * @return the int
     */
    public static int readItemStackTag(ItemStack itemStack, String nameTag) {
        if (itemStack != null && itemStack.hasTag()) {
            CompoundTag nbt = itemStack.getTag();
            if (nbt != null && nbt.contains(nameTag)) {
                return nbt.getInt(nameTag);
            }
        }
        return 0;
    }

    /**
     * Read entity tag int.
     *
     * @param entity  the entity
     * @param nameTag the name tag
     * @return the int
     */
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
