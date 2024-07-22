package com.dod.UnrealZaruba.Utils.DataStructures;

import java.util.function.Consumer;

import com.dod.UnrealZaruba.unrealzaruba;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.server.ServerLifecycleHooks;

public class BlockVolume {
    private final BlockPos minPos;
    private final BlockPos maxPos;
    private final BlockPos center;
    private final int blockAmount;
    
    public int GetBlockAmount() {
        return blockAmount;
    }

    public BlockPos GetCenter() {
        return center;
    }

    public BlockVolume(BlockPos minPos, BlockPos maxPos) {
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.blockAmount = countBlocksInside();

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

    public int countBlocksInside() {
        int count = 0;
        for (int x = minPos.getX(); x < maxPos.getX(); x++) {
            for (int y = minPos.getY(); y < maxPos.getY(); y++) {
                for (int z = minPos.getZ(); z < maxPos.getZ(); z++) {
                    ServerLevel world = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
                    if (world == null){
                        unrealzaruba.LOGGER.warn("[Ай, бля] World not found");
                        return 0;
                    }
                    BlockPos position = new BlockPos(x, y, z);
                    BlockState blockState = world.getBlockState(position);
                    if (blockState.getBlock() != Blocks.AIR) {
                        count++;   
                    }
                }
            }
        }
        return count;
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
