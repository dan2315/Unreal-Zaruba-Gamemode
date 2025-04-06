package com.dod.UnrealZaruba.CharacterClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class CharacterClassData {
    private final String nameId;
    private final String displayName;
    private final List<ItemStack> kit;

    public CharacterClassData(String nameId, String displayName) {
        this.nameId = nameId;
        this.displayName = displayName;
        this.kit = new ArrayList<>();
    }

    public String getNameId() {
        return nameId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CharacterClassData addKitItem(ResourceLocation item, int count) {
        kit.add(new ItemStack(ForgeRegistries.ITEMS.getValue(item), count));
        return this;
    }

    public CharacterClassData addKitItem(Item item, int count) {
        kit.add(new ItemStack(item, count));
        return this;
    }

    public CharacterClassData addKitItem(ItemStack itemStack) {
        kit.add(itemStack);
        return this;
    }

    public List<ItemStack> getKit() {
        return kit;
    }
} 