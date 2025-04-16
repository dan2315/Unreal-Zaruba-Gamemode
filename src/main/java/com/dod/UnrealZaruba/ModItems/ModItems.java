package com.dod.UnrealZaruba.ModItems;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * <h1>Main mod's items DeferredRegisterer</h1>
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            UnrealZaruba.MOD_ID);

    public static final RegistryObject<Item> TENT = ModItems.ITEMS.register("tent",
            () -> new HandTent(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> HAND_ASSEMBLER = ModItems.ITEMS.register("hand_assembler",
            () -> new HandAssembler(new Item.Properties()));
    public static final RegistryObject<Item> SKULL = ModItems.ITEMS.register("skull",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RED_BLOCK_ITEM = ModItems.ITEMS.register("red_block",
            () -> new BlockItem(ModBlocks.RED_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLUE_BLOCK_ITEM = ModItems.ITEMS.register("blue_block",
            () -> new BlockItem(ModBlocks.BLUE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> RED_SPAWN_ITEM = ModItems.ITEMS.register("red_spawn_block",
            () -> new BlockItem(ModBlocks.RED_SPAWN_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLUE_SPAWN_ITEM = ModItems.ITEMS.register("blue_spawn_block",
            () -> new BlockItem(ModBlocks.BLUE_SPAWN_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> SEAT_WITHOUT_COLLISION_ITEM = ModItems.ITEMS.register("seat_without_collision",
            () -> new BlockItem(ModBlocks.SEAT_WITHOUT_COLLISION.get(), new Item.Properties()));
    public static final RegistryObject<Item> TENT_MAIN_BLOCK_BLUE_ITEM = ModItems.ITEMS.register("tent_main_block_blue",
            () -> new BlockItem(ModBlocks.TENT_MAIN_BLOCK_BLUE.get(), new Item.Properties()));
    public static final RegistryObject<Item> TENT_MAIN_BLOCK_RED_ITEM = ModItems.ITEMS.register("tent_main_block_red",
            () -> new BlockItem(ModBlocks.TENT_MAIN_BLOCK_RED.get(), new Item.Properties()));
    public static final RegistryObject<Item> VEHICLE_PURCHASE_BLOCK_ITEM = ModItems.ITEMS.register("vehicle_purchase_block",
            () -> new BlockItem(ModBlocks.VEHICLE_PURCHASE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> CLASS_ASSIGNER_BLOCK_ITEM = ModItems.ITEMS.register("class_assigner_block",
            () -> new BlockItem(ModBlocks.CLASS_ASSIGNER_BLOCK.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ModItems.ITEMS.register(eventBus);
    }
}
