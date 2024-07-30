package com.dod.UnrealZaruba.ModBlocks;

import com.dod.UnrealZaruba.unrealzaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.ModItems.ModItems;
import com.jozufozu.flywheel.util.Color;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    
    public static final DeferredRegister <Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, unrealzaruba.MOD_ID);

    public static final RegistryObject<Block> RED_BLOCK = ModBlocks.BLOCKS.register("red_block",
            () -> new TeamBlock(TeamColor.RED,  Block.Properties.of(Material.WOOL)));
    public static final RegistryObject<Block> BLUE_BLOCK = ModBlocks.BLOCKS.register("blue_block",
            () -> new TeamBlock(TeamColor.BLUE,  Block.Properties.of(Material.WOOL)));
    public static final RegistryObject<Block> SOLID_HOPPER = ModBlocks.BLOCKS.register("solid_hopper",
            () -> new SolidHopperBlock());
    public static final RegistryObject<Block> RED_SPAWN_BLOCK = ModBlocks.BLOCKS.register("red_spawn_block",
            () -> new SpawnBlock(TeamColor.RED, Block.Properties.of(Material.WOOL)));
    public static final RegistryObject<Block> BLUE_SPAWN_BLOCK = ModBlocks.BLOCKS.register("blue_spawn_block",
            () -> new SpawnBlock(TeamColor.BLUE, Block.Properties.of(Material.WOOL)));
    public static final RegistryObject<Block> SEAT_WITHOUT_COLLISION = ModBlocks.BLOCKS.register("seat_without_collision",
            () -> new SeatWithoutCollision(BlockBehaviour.Properties.of(Material.WOOD)
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(), DyeColor.WHITE, false));

    public static void register(IEventBus eventBus) {
        ModBlocks.BLOCKS.register(eventBus);
    }
}
