package com.dod.UnrealZaruba.ModItems;

import com.dod.UnrealZaruba.unrealzaruba;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            unrealzaruba.MOD_ID);

    public static final RegistryObject<Item> TENT = ModItems.ITEMS.register("tent",
            () -> new HandTent(new Item.Properties().rarity(Rarity.EPIC).tab(CreativeTabs.MAIN_TAB)));

    public static final RegistryObject<Item> HAND_ASSEMBLER = ModItems.ITEMS.register("hand_assembler",
            () -> new HandAssembler(new Item.Properties().tab(CreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> RED_BLOCK_ITEM = ModItems.ITEMS.register("red_block",
            () -> new BlockItem(ModBlocks.RED_BLOCK.get(), new Item.Properties().tab(CreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> BLUE_BLOCK_ITEM = ModItems.ITEMS.register("blue_block",
            () -> new BlockItem(ModBlocks.BLUE_BLOCK.get(), new Item.Properties().tab(CreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> SOLID_HOPPER_ITEM = ModItems.ITEMS.register("solid_hopper",
            () -> new BlockItem(ModBlocks.SOLID_HOPPER.get(), new Item.Properties().tab(CreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> RED_SPAWN_ITEM = ModItems.ITEMS.register("red_spawn_block",
            () -> new BlockItem(ModBlocks.RED_SPAWN_BLOCK.get(), new Item.Properties().tab(CreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> BLUE_SPAWN_ITEM = ModItems.ITEMS.register("blue_spawn_block",
            () -> new BlockItem(ModBlocks.BLUE_SPAWN_BLOCK.get(), new Item.Properties().tab(CreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> SEAT_WITHOUT_COLLISION_ITEM = ModItems.ITEMS.register("seat_without_collision",
            () -> new BlockItem(ModBlocks.SEAT_WITHOUT_COLLISION.get(), new Item.Properties().tab(CreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> TENT_MAIN_BLOCK_BLUE_ITEM = ModItems.ITEMS.register("tent_main_block_blue",
            () -> new BlockItem(ModBlocks.TENT_MAIN_BLOCK_BLUE.get(), new Item.Properties().tab(CreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> TENT_MAIN_BLOCK_RED_ITEM = ModItems.ITEMS.register("tent_main_block_red",
            () -> new BlockItem(ModBlocks.TENT_MAIN_BLOCK_RED.get(), new Item.Properties().tab(CreativeTabs.MAIN_TAB)));



    public static void register(IEventBus eventBus) {
        ModItems.ITEMS.register(eventBus);
    }
}
