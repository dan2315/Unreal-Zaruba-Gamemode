package com.dod.UnrealZaruba.Vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.registries.ForgeRegistries;

public class VehicleData {
    private final ResourceLocation schematicLocation;
    private final String name;
    private final Map<ResourceLocation, Integer> requiredItems;

    public ResourceLocation getSchematicLocation() {
        return schematicLocation;
    }

    public ResourceLocation getKey() {
        return schematicLocation; // mega lul
    }

    public String getName() {
        return name;
    }

    public VehicleData(ResourceLocation schematicLocation, String name) {
        this.schematicLocation = schematicLocation;
        this.name = name;
        this.requiredItems = new HashMap<>();
    }

    public VehicleData addItemRequirement(ResourceLocation item, int count) {
        requiredItems.put(item, count);
        return this;
    }

    public List<ItemStack> getRequiredItemStacks() {
        List<ItemStack> result = new ArrayList<>();
        
        for (Map.Entry<ResourceLocation, Integer> entry : requiredItems.entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.getKey());
            if (item != null) {
                result.add(new ItemStack(item, entry.getValue()));
            }
        }
        
        return result;
    }
    
    public boolean hasRequiredItems(Inventory inventory) {
        for (Map.Entry<ResourceLocation, Integer> entry : requiredItems.entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.getKey());
            if (item == null || inventory.countItem(item) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    public boolean consumeRequiredItems(Inventory inventory) {
        if (!hasRequiredItems(inventory)) {
            return false;
        }
        
        for (Map.Entry<ResourceLocation, Integer> entry : requiredItems.entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.getKey());
            if (item != null) {
                inventory.clearOrCountMatchingItems(stack -> stack.is(item), entry.getValue(), inventory.player.inventoryMenu.getCraftSlots());
            }
        }
        
        return true;
    }
    
}
