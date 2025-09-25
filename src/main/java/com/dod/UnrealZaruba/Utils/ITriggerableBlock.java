package com.dod.unrealzaruba.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for blocks that can be triggered by groups when something happens.
 */
public interface ITriggerableBlock {
    void trigger(Level level, BlockPos pos, BlockState state);
} 