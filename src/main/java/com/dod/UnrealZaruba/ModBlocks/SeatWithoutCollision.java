package com.dod.UnrealZaruba.ModBlocks;


import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SeatWithoutCollision extends SeatBlock {

    VoxelShape selectionShape = Shapes.box(0.6, 0.0, 0.6, 0.9, 0.5, 0.9);

    public SeatWithoutCollision(Properties properties, DyeColor color) {
        super(properties, color);
    }

    @Override
    public VoxelShape getShape(BlockState p_220053_1_, BlockGetter p_220053_2_, BlockPos p_220053_3_, CollisionContext p_220053_4_) {
        return selectionShape;
    }

}