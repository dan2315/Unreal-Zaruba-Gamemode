package com.dod.unrealzaruba.ModBlocks.Parachute;

import com.dod.unrealzaruba.ModBlocks.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A small parachute pack that sits on top of a physics body (the supply drop's container cube). Once deployed its
 * block entity slows the whole body's fall and a canopy is drawn above it. Always sits on the floor of its block;
 * put it on the top face, above the body's centre of mass, so the load hangs level under the canopy.
 */
public class ParachutePackBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 4, 12);

    public ParachutePackBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.0F).sound(SoundType.WOOL).noOcclusion());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ParachutePackBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlocks.PARACHUTE_PACK_BLOCK_ENTITY.get()) return null;
        return (world, pos, blockState, blockEntity) -> ((ParachutePackBlockEntity) blockEntity).tick();
    }
}
