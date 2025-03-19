package com.dod.UnrealZaruba.ModBlocks;

import com.dod.UnrealZaruba.ModBlocks.TeamBlock.SpawnBlock;
import com.dod.UnrealZaruba.ModBlocks.TeamBlock.TeamBlock;
import com.dod.UnrealZaruba.ModBlocks.Tent.TentMainBlock;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister <Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, UnrealZaruba.MOD_ID);

    public static final RegistryObject<Block> RED_BLOCK = ModBlocks.BLOCKS.register("red_block",
            () -> new TeamBlock(TeamColor.RED,  Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final RegistryObject<Block> BLUE_BLOCK = ModBlocks.BLOCKS.register("blue_block",
            () -> new TeamBlock(TeamColor.BLUE,  Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final RegistryObject<Block> RED_SPAWN_BLOCK = ModBlocks.BLOCKS.register("red_spawn_block",
            () -> new SpawnBlock(TeamColor.RED, Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final RegistryObject<Block> BLUE_SPAWN_BLOCK = ModBlocks.BLOCKS.register("blue_spawn_block",
            () -> new SpawnBlock(TeamColor.BLUE, Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final RegistryObject<Block> SEAT_WITHOUT_COLLISION = ModBlocks.BLOCKS.register("seat_without_collision",
            () -> new SeatWithoutCollision(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(), DyeColor.WHITE));
    public static final RegistryObject<Block> TENT_MAIN_BLOCK_BLUE = ModBlocks.BLOCKS.register("tent_main_block_blue",
            () -> new TentMainBlock(TeamColor.BLUE));
    public static final RegistryObject<Block> TENT_MAIN_BLOCK_RED = ModBlocks.BLOCKS.register("tent_main_block_red",
            () -> new TentMainBlock(TeamColor.RED));


    public static void register(IEventBus eventBus) {
        ModBlocks.BLOCKS.register(eventBus);
    }
}
