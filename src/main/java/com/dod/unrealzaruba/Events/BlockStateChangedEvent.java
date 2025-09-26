package com.dod.unrealzaruba.Events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

public class BlockStateChangedEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final BlockState newState;

    public BlockStateChangedEvent(Level level, BlockPos pos, BlockState newState) {
        this.level = level;
        this.pos = pos.immutable();
        this.newState = newState;
    }

    public Level getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public BlockState getNewState() { return newState; }
}
