package com.dod.UnrealZaruba.ModBlocks;

import javax.annotation.Nullable;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ModBlocks.BlockEntities.VehiclePurchaseBlockEntity;
import com.dod.UnrealZaruba.VsIntegration.ShipCreator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3i;

public class VehiclePurchaseBlock extends Block implements EntityBlock {
    private ResourceLocation schematicLocation = new ResourceLocation(UnrealZaruba.MOD_ID, "schematics/test.nbt");

    public VehiclePurchaseBlock() {
        super(
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                        .strength(2.0f, 3.0f)
                        .sound(SoundType.METAL));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new VehiclePurchaseBlockEntity(blockPos, blockState, schematicLocation);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
                return level.isClientSide ? 
                createTickerHelper(type, ModBlocks.VEHICLE_PURCHASE_BLOCK_ENTITY.get(), 
                    (world, pos, blockState, be) -> be.clientTick()) : null;
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<E> ticker) {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            Vector3i position = new Vector3i(pos.getX(), pos.getY(), pos.getZ());
            ShipCreator.CreateShipFromTemplate(position, schematicLocation, (ServerLevel) level);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
