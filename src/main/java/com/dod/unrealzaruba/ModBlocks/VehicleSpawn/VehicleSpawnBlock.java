package com.dod.unrealzaruba.ModBlocks.VehicleSpawn;

import com.dod.unrealzaruba.utils.ITriggerableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

public class VehicleSpawnBlock extends HorizontalDirectionalBlock implements EntityBlock, ITriggerableBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    public VehicleSpawnBlock() {
        super(Properties.of().mapColor(MapColor.METAL).strength(3.5F));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VehicleSpawnBlockEntity(pos, state);
    }

    @Override
    public void trigger(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof VehicleSpawnBlockEntity blockEntity) {
            blockEntity.spawnVehicle();
        }
    }
}