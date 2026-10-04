package com.dod.unrealzaruba.utils;

import net.minecraft.core.BlockPos;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Random;

public class FireworkLauncher {

    private static final Random RANDOM = new Random();

    public static void launchFireworks(ServerLevel world, BlockPos pos, int radius) {
        for (int i = 0; i < 5; i++) {
            int offsetX = (int)(RANDOM.nextDouble() * radius * 2 - radius);
            int offsetZ = (int)(RANDOM.nextDouble() * radius * 2 - radius);
            BlockPos fireworkPos = pos.offset(offsetX, 0, offsetZ);
            launchFirework(world, fireworkPos);
        }
    }

    private static void launchFirework(ServerLevel world, BlockPos pos) {
        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET, 1);
        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.values()[RANDOM.nextInt(FireworkExplosion.Shape.values().length)], // Random shape
                IntList.of(RANDOM.nextInt(0xFFFFFF)), // Random color
                IntList.of(), false, false);
        fireworkStack.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion))); // Flight duration 1

        FireworkRocketEntity fireworkEntity = new FireworkRocketEntity(world, pos.getX(), pos.getY(), pos.getZ(), fireworkStack);
        world.addFreshEntity(fireworkEntity);
    }
}
