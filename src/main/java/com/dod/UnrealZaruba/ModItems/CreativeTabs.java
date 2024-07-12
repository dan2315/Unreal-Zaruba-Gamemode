package com.dod.UnrealZaruba.ModItems;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CreativeTabs {
    public static final CreativeModeTab MAIN_TAB = new CreativeModeTab("Unreal Zalupa") {

        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.HAND_ASSEMBLER.get());
        }
        
    };
}
