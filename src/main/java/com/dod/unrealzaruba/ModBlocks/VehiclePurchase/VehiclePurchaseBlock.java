package com.dod.unrealzaruba.ModBlocks.VehiclePurchase;

import com.dod.unrealzaruba.Economy.EconomyData;
import com.dod.unrealzaruba.UI.VehiclePurchaseMenu.VehiclePurchaseMenu;
import javax.annotation.Nullable;

import com.dod.unrealzaruba.ModBlocks.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;

public class VehiclePurchaseBlock extends Block implements EntityBlock {

    public VehiclePurchaseBlock() {
        super(
        BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(-1.0F, 3600000.0F)
        .sound(SoundType.METAL));

        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new VehiclePurchaseBlockEntity(blockPos, blockState);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlocks.VEHICLE_PURCHASE_BLOCK_ENTITY.get() ? 
                    (BlockEntityTicker<T>) (world, pos, blockState, be) ->
                    ((VehiclePurchaseBlockEntity) be).serverTick() : null; 
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<E> ticker) {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof VehiclePurchaseBlockEntity) {
                player.openMenu((MenuProvider) blockEntity, buf -> VehiclePurchaseMenu.writeExtraData(buf, pos, VehiclePurchaseMenu.offersFor(EconomyData.current())));
            } 
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
