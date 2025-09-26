package com.dod.unrealzaruba.CharacterClass.ItemDataBuilders;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class ProtectionPixel {

    public static final class Armor {
        public static final String HELMET = "protection_pixel:helmet";
        public static final String CHESTPLATE = "protection_pixel:chestplate";
        public static final String LEGGINGS = "protection_pixel:leggings";
        public static final String BOOTS = "protection_pixel:socks_boots";
    }

    public static ItemStack CreatePowerEngineWithWaterTank() {

        ItemStack powerEngine = new ItemStack(ForgeRegistries.ITEMS.getValue(
            new ResourceLocation("protection_pixel:powerengine")), 1);
        
        CompoundTag mainTag = new CompoundTag();
        mainTag.putString("id", "protection_pixel:powerengine");
        mainTag.putInt("Count", 1);
        mainTag.putDouble("count", 0.0d);
        
        CompoundTag inventoryTag = new CompoundTag();
        inventoryTag.putInt("Size", 9);
        
        ListTag itemsList = new ListTag();
        
        CompoundTag waterTankSlot = new CompoundTag();
        waterTankSlot.putInt("Slot", 0);
        waterTankSlot.putString("id", "protection_pixel:watertank");
        waterTankSlot.putInt("Count", 1);
        
        CompoundTag waterTankTag = new CompoundTag();
        waterTankTag.putInt("Damage", 0);
        waterTankSlot.put("tag", waterTankTag);
        
        itemsList.add(waterTankSlot);
        
        for (int slot = 1; slot <= 8; slot++) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("Slot", slot);
            slotTag.putString("id", "protection_pixel:flarerod");
            slotTag.putInt("Count", 1);
            
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Damage", 0);
            slotTag.put("tag", itemTag);
            
            itemsList.add(slotTag);
        }
        
        inventoryTag.put("Items", itemsList);
        mainTag.put("Inventory", inventoryTag);
        
        mainTag.putDouble("steampower", 8.0d);
        
        powerEngine.setTag(mainTag);
        return powerEngine;
    }
}
