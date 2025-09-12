package com.dod.UnrealZaruba.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public class BarrierRemovalTask {
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);


    public static void removeBarriersAsync(ServerLevel world, BlockVolume barrierVolume) {
        int minX = barrierVolume.getMinPos().getX();
        int minY = barrierVolume.getMinPos().getY();
        int minZ = barrierVolume.getMinPos().getZ();
        int maxX = barrierVolume.getMaxPos().getX();
        int maxY = barrierVolume.getMaxPos().getY();
        int maxZ = barrierVolume.getMaxPos().getZ();

        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        int batchSize = totalBlocks / THREAD_COUNT;


        for (int i = 0; i < THREAD_COUNT; i++) {
            int start = i * batchSize;
            int end = (i == THREAD_COUNT - 1) ? totalBlocks : (i + 1) * batchSize;

            executorService.submit(() -> {
                for (int j = start; j < end; j++) {
                    int x = minX + (j % (maxX - minX + 1));
                    int y = minY + ((j / (maxX - minX + 1)) % (maxY - minY + 1));
                    int z = minZ + (j / ((maxX - minX + 1) * (maxY - minY + 1)));

                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockState(pos).getBlock() == Blocks.BARRIER) {
                        world.getServer().execute(() -> world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2));
                    }
                }
            });
        }
    }
}