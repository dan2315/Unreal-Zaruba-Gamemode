package com.dod.unrealzaruba.ModBlocks;

import com.dod.unrealzaruba.ModBlocks.TeamBlock.SpawnBlock;
import com.dod.unrealzaruba.ModBlocks.TeamBlock.TeamBlock;
import com.dod.unrealzaruba.ModBlocks.Tent.TentMainBlock;
import com.dod.unrealzaruba.ModBlocks.VehiclePurchase.VehiclePurchaseBlock;
import com.dod.unrealzaruba.ModBlocks.VehiclePurchase.VehiclePurchaseBlockEntity;
import com.dod.unrealzaruba.ModBlocks.VehicleSpawn.VehicleSpawnBlock;
import com.dod.unrealzaruba.ModBlocks.VehicleSpawn.VehicleSpawnBlockEntity;
import com.dod.unrealzaruba.Commands.Arguments.TeamColor;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.dod.unrealzaruba.ModBlocks.ClassAssignerBlock.ClassAssignerBlockEntity;
import com.dod.unrealzaruba.ModBlocks.ClassAssignerBlock.ClassAssignerBlock;

public class ModBlocks {

    public static final DeferredRegister <Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, UnrealZaruba.MOD_ID);

    public static final DeferredHolder<Block, Block> RED_BLOCK = ModBlocks.BLOCKS.register("red_block",
            () -> new TeamBlock(TeamColor.RED,  Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final DeferredHolder<Block, Block> BLUE_BLOCK = ModBlocks.BLOCKS.register("blue_block",
            () -> new TeamBlock(TeamColor.BLUE,  Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final DeferredHolder<Block, Block> YELLOW_BLOCK = ModBlocks.BLOCKS.register("yellow_block",
            () -> new TeamBlock(TeamColor.YELLOW,  Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final DeferredHolder<Block, Block> RED_SPAWN_BLOCK = ModBlocks.BLOCKS.register("red_spawn_block",
            () -> new SpawnBlock(TeamColor.RED, Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final DeferredHolder<Block, Block> BLUE_SPAWN_BLOCK = ModBlocks.BLOCKS.register("blue_spawn_block",
            () -> new SpawnBlock(TeamColor.BLUE, Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final DeferredHolder<Block, Block> SEAT_WITHOUT_COLLISION = ModBlocks.BLOCKS.register("seat_without_collision",
            () -> new SeatWithoutCollision(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(), DyeColor.WHITE));
    public static final DeferredHolder<Block, Block> TENT_MAIN_BLOCK_BLUE = ModBlocks.BLOCKS.register("tent_main_block_blue",
            () -> new TentMainBlock(TeamColor.BLUE));
    public static final DeferredHolder<Block, Block> TENT_MAIN_BLOCK_RED = ModBlocks.BLOCKS.register("tent_main_block_red",
            () -> new TentMainBlock(TeamColor.RED));
    public static final DeferredHolder<Block, Block> VEHICLE_PURCHASE_BLOCK = ModBlocks.BLOCKS.register("vehicle_purchase_block",
            () -> new VehiclePurchaseBlock());
    public static final DeferredHolder<Block, Block> CLASS_ASSIGNER_BLOCK = ModBlocks.BLOCKS.register("class_assigner_block",
            () -> new ClassAssignerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)));
    public static final DeferredHolder<Block, Block> VEHICLE_SPAWN_BLOCK = ModBlocks.BLOCKS.register("vehicle_spawn_block",
            () -> new VehicleSpawnBlock());
    public static final DeferredHolder<Block, Block> PARACHUTE_PACK_BLOCK = ModBlocks.BLOCKS.register("parachute_pack",
            () -> new com.dod.unrealzaruba.ModBlocks.Parachute.ParachutePackBlock());


    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES 
    = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, UnrealZaruba.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VehiclePurchaseBlockEntity>> VEHICLE_PURCHASE_BLOCK_ENTITY = BLOCK_ENTITIES.register("vehicle_purchase_block_entity",
            () -> BlockEntityType.Builder.of(VehiclePurchaseBlockEntity::new, ModBlocks.VEHICLE_PURCHASE_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClassAssignerBlockEntity>> CLASS_ASSIGNER_BLOCK_ENTITY = BLOCK_ENTITIES.register("class_assigner_block_entity",
            () -> BlockEntityType.Builder.of(ClassAssignerBlockEntity::new, ModBlocks.CLASS_ASSIGNER_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VehicleSpawnBlockEntity>> VEHICLE_SPAWN_BLOCK_ENTITY = BLOCK_ENTITIES.register("vehicle_spawn_block_entity",
            () -> BlockEntityType.Builder.of(VehicleSpawnBlockEntity::new, ModBlocks.VEHICLE_SPAWN_BLOCK.get()).build(null));


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.dod.unrealzaruba.ModBlocks.Parachute.ParachutePackBlockEntity>> PARACHUTE_PACK_BLOCK_ENTITY = BLOCK_ENTITIES.register("parachute_pack_block_entity",
            () -> BlockEntityType.Builder.of(com.dod.unrealzaruba.ModBlocks.Parachute.ParachutePackBlockEntity::new, ModBlocks.PARACHUTE_PACK_BLOCK.get()).build(null));


    public static void register(IEventBus eventBus) {
        ModBlocks.BLOCKS.register(eventBus);
        ModBlocks.BLOCK_ENTITIES.register(eventBus);
    }
}
