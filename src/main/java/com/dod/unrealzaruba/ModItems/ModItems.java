package com.dod.unrealzaruba.ModItems;

import com.dod.unrealzaruba.ModBlocks.ModBlocks;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * <h1>Main mod's items DeferredRegisterer</h1>
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, UnrealZaruba.MOD_ID);

    public static final DeferredHolder<Item, Item> TENT = ModItems.ITEMS.register("tent",
            () -> new HandTent(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, Item> HAND_ASSEMBLER = ModItems.ITEMS.register("hand_assembler",
            () -> new HandAssembler(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RED_BLOCK_ITEM = ModItems.ITEMS.register("red_block",
            () -> new BlockItem(ModBlocks.RED_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> BLUE_BLOCK_ITEM = ModItems.ITEMS.register("blue_block",
            () -> new BlockItem(ModBlocks.BLUE_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> YELLOW_BLOCK_ITEM = ModItems.ITEMS.register("yellow_block",
            () -> new BlockItem(ModBlocks.YELLOW_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> RED_SPAWN_ITEM = ModItems.ITEMS.register("red_spawn_block",
            () -> new BlockItem(ModBlocks.RED_SPAWN_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> BLUE_SPAWN_ITEM = ModItems.ITEMS.register("blue_spawn_block",
            () -> new BlockItem(ModBlocks.BLUE_SPAWN_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> SEAT_WITHOUT_COLLISION_ITEM = ModItems.ITEMS.register("seat_without_collision",
            () -> new BlockItem(ModBlocks.SEAT_WITHOUT_COLLISION.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> TENT_MAIN_BLOCK_BLUE_ITEM = ModItems.ITEMS.register("tent_main_block_blue",
            () -> new BlockItem(ModBlocks.TENT_MAIN_BLOCK_BLUE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> TENT_MAIN_BLOCK_RED_ITEM = ModItems.ITEMS.register("tent_main_block_red",
            () -> new BlockItem(ModBlocks.TENT_MAIN_BLOCK_RED.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> VEHICLE_PURCHASE_BLOCK_ITEM = ModItems.ITEMS.register("vehicle_purchase_block",
            () -> new BlockItem(ModBlocks.VEHICLE_PURCHASE_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLASS_ASSIGNER_BLOCK_ITEM = ModItems.ITEMS.register("class_assigner_block",
            () -> new BlockItem(ModBlocks.CLASS_ASSIGNER_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> VEHICLE_SPAWN_BLOCK_ITEM = ModItems.ITEMS.register("vehicle_spawn_block",
            () -> new BlockItem(ModBlocks.VEHICLE_SPAWN_BLOCK.get(), new Item.Properties()));

    /** An op's map editor: capture points, targets to destroy and team bases of the open map (see MapEditorClient). */
    public static final DeferredHolder<Item, Item> MAP_EDITOR = ModItems.ITEMS.register("map_editor",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, Item> PARACHUTE_PACK_ITEM = ModItems.ITEMS.register("parachute_pack",
            () -> new BlockItem(ModBlocks.PARACHUTE_PACK_BLOCK.get(), new Item.Properties()));

    /** The case from a supply drop: carried to the base to cash in. */
    public static final DeferredHolder<Item, Item> SUPPLY_CASE = ModItems.ITEMS.register("supply_case",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
                                            java.util.List<net.minecraft.network.chat.Component> tooltip,
                                            net.minecraft.world.item.TooltipFlag flag) {
                    tooltip.add(net.minecraft.network.chat.Component.translatable("item.unrealzaruba.supply_case.desc")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            });

    /** Shows the size, price and publishing problems of a Sable contraption. */
    public static final DeferredHolder<Item, Item> CONTRAPTION_SCANNER = ModItems.ITEMS.register("contraption_scanner",
            () -> new ContraptionScannerItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ModItems.ITEMS.register(eventBus);
    }
}
