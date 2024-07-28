package com.dod.UnrealZaruba.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public class BarrierRemovalTask {
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);


    public static void removeBarriersAsync(ServerLevel world, BlockPos pos1, BlockPos pos2) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        int batchSize = totalBlocks / THREAD_COUNT;

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            int start = i * batchSize;
            int end = (i == THREAD_COUNT - 1) ? totalBlocks : (i + 1) * batchSize;

            Future<?> future = executorService.submit(() -> {
                for (int j = start; j < end; j++) {
                    int x = minX + (j % (maxX - minX + 1));
                    int y = minY + ((j / (maxX - minX + 1)) % (maxY - minY + 1));
                    int z = minZ + (j / ((maxX - minX + 1) * (maxY - minY + 1)));

                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockState(pos).getBlock() == Blocks.BARRIER) {
                        // Schedule block removal to be executed on the main thread
                        world.getServer().execute(() -> world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2));
                    }
                }
            });

            futures.add(future);
        }

        // Optionally wait for all futures to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}