package com.dod.UnrealZaruba.ModItems;

import javax.annotation.Nonnull;

import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;

import com.dod.UnrealZaruba.unrealzaruba;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            unrealzaruba.MOD_ID);

    public static final RegistryObject<Item> HAND_ASSEMBLER = ModItems.ITEMS.register("hand_assembler",
            () -> new HandAssembler(new Item.Properties().tab(CreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> RED_BLOCK_ITEM = ModItems.ITEMS.register("red_block",
            () -> new BlockItem(ModBlocks.RED_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    public static final RegistryObject<Item> BLUE_BLOCK_ITEM = ModItems.ITEMS.register("blue_block",
            () -> new BlockItem(ModBlocks.BLUE_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    public static final RegistryObject<Item> SOLID_HOPPER_ITEM = ModItems.ITEMS.register("solid_hopper",
            () -> new BlockItem(ModBlocks.SOLID_HOPPER.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    public static final RegistryObject<Item> RED_SPAWN_ITEM = ModItems.ITEMS.register("red_spawn_block",
            () -> new BlockItem(ModBlocks.RED_SPAWN_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    public static final RegistryObject<Item> BLUE_SPAWN_ITEM = ModItems.ITEMS.register("blue_spawn_block",
            () -> new BlockItem(ModBlocks.BLUE_SPAWN_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    public static final RegistryObject<Item> SEAT_WITHOUT_COLLISION_ITEM = ModItems.ITEMS.register("seat_without_collision",
            () -> new BlockItem(ModBlocks.SEAT_WITHOUT_COLLISION.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));


    public static void register(IEventBus eventBus) {
        ModItems.ITEMS.register(eventBus);
    }
}
