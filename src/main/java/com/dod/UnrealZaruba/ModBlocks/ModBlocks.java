package com.dod.UnrealZaruba.ModBlocks;

import com.dod.UnrealZaruba.ModBlocks.TeamBlock.SpawnBlock;
import com.dod.UnrealZaruba.ModBlocks.TeamBlock.TeamBlock;
import com.dod.UnrealZaruba.ModBlocks.Tent.TentMainBlock;
import com.dod.UnrealZaruba.ModBlocks.VehiclePurchase.VehiclePurchaseBlock;
import com.dod.UnrealZaruba.ModBlocks.VehiclePurchase.VehiclePurchaseBlockEntity;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnBlock;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.dod.UnrealZaruba.ModBlocks.ClassAssignerBlock.ClassAssignerBlockEntity;
import com.dod.UnrealZaruba.ModBlocks.ClassAssignerBlock.ClassAssignerBlock;

public class ModBlocks {

    public static final DeferredRegister <Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, UnrealZaruba.MOD_ID);

    public static final RegistryObject<Block> RED_BLOCK = ModBlocks.BLOCKS.register("red_block",
            () -> new TeamBlock(TeamColor.RED,  Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final RegistryObject<Block> BLUE_BLOCK = ModBlocks.BLOCKS.register("blue_block",
            () -> new TeamBlock(TeamColor.BLUE,  Block.Properties.of().mapColor(MapColor.WOOL)));
    public static final RegistryObject<Block> YELLOW_BLOCK = ModBlocks.BLOCKS.register("yellow_block",
            () -> new TeamBlock(TeamColor.YELLOW,  Block.Properties.of().mapColor(MapColor.WOOL)));
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
    public static final RegistryObject<Block> VEHICLE_PURCHASE_BLOCK = ModBlocks.BLOCKS.register("vehicle_purchase_block",
            () -> new VehiclePurchaseBlock());
    public static final RegistryObject<Block> CLASS_ASSIGNER_BLOCK = ModBlocks.BLOCKS.register("class_assigner_block",
            () -> new ClassAssignerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)));
    public static final RegistryObject<Block> VEHICLE_SPAWN_BLOCK = ModBlocks.BLOCKS.register("vehicle_spawn_block",
            () -> new VehicleSpawnBlock());


    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES 
    = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, UnrealZaruba.MOD_ID);

    public static final RegistryObject<BlockEntityType<VehiclePurchaseBlockEntity>> VEHICLE_PURCHASE_BLOCK_ENTITY = BLOCK_ENTITIES.register("vehicle_purchase_block_entity",
            () -> BlockEntityType.Builder.of(VehiclePurchaseBlockEntity::new, ModBlocks.VEHICLE_PURCHASE_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<ClassAssignerBlockEntity>> CLASS_ASSIGNER_BLOCK_ENTITY = BLOCK_ENTITIES.register("class_assigner_block_entity",
            () -> BlockEntityType.Builder.of(ClassAssignerBlockEntity::new, ModBlocks.CLASS_ASSIGNER_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<VehicleSpawnBlockEntity>> VEHICLE_SPAWN_BLOCK_ENTITY = BLOCK_ENTITIES.register("vehicle_spawn_block_entity",
            () -> BlockEntityType.Builder.of(VehicleSpawnBlockEntity::new, ModBlocks.VEHICLE_SPAWN_BLOCK.get()).build(null));


    public static void register(IEventBus eventBus) {
        ModBlocks.BLOCKS.register(eventBus);
        ModBlocks.BLOCK_ENTITIES.register(eventBus);
    }
}
