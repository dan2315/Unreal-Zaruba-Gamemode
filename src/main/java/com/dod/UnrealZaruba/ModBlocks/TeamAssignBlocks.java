package com.dod.UnrealZaruba.ModBlocks;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.ModItems.ModItems;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

public class TeamAssignBlocks {

    public static final RegistryObject<Block> RED_BLOCK = ModBlocks.BLOCKS.register("red_block",
            () -> new TeamBlock(TeamColor.RED,  Block.Properties.of(Material.WOOL)));
    public static final RegistryObject<Block> BLUE_BLOCK = ModBlocks.BLOCKS.register("blue_block",
            () -> new TeamBlock(TeamColor.BLUE,  Block.Properties.of(Material.WOOL)));
    public static final RegistryObject<Block> SOLID_HOPPER = ModBlocks.BLOCKS.register("solid_hopper",
            () -> new SolidHopperBlock());


    public static final RegistryObject<Item> RED_BLOCK_ITEM = ModItems.ITEMS.register("red_block",
            () -> new BlockItem(RED_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    public static final RegistryObject<Item> BLUE_BLOCK_ITEM = ModItems.ITEMS.register("blue_block",
            () -> new BlockItem(BLUE_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    public static final RegistryObject<Item> SOLID_HOPPER_ITEM = ModItems.ITEMS.register("solid_hopper",
            () -> new BlockItem(SOLID_HOPPER.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));



    public static void register(IEventBus eventBus) {
        ModBlocks.BLOCKS.register(eventBus);
        ModItems.ITEMS.register(eventBus);
    }
}
