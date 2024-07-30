package com.dod.UnrealZaruba.Utils.DataStructures;

import java.util.function.Consumer;


import net.minecraft.core.BlockPos;

public class BlockVolume {
    private final BlockPos minPos;
    private final BlockPos maxPos;
    private final BlockPos center;

    public BlockPos GetCenter() {
        return center;
    }

    public BlockVolume(BlockPos pos1, BlockPos pos2, boolean countBlocks) {
        this.minPos = new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()));
        this.maxPos = new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ()));

        center = new BlockPos(
                minPos.getX() + (maxPos.getX() - minPos.getX())/ 2,
                minPos.getY(),
                minPos.getZ() + (maxPos.getZ() - minPos.getZ())/ 2);
    }

    public boolean isWithinVolume(BlockPos pos) {
        return pos.getX() >= minPos.getX() && pos.getX() <= maxPos.getX() &&
               pos.getY() >= minPos.getY() && pos.getY() <= maxPos.getY() &&
               pos.getZ() >= minPos.getZ() && pos.getZ() <= maxPos.getZ();
    }

    public void ForEachBlock(Consumer<BlockPos> event) {
        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
            for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    BlockPos position = new BlockPos(x, y, z);
                    event.accept(position);
                }
            }
        }
    }
}
