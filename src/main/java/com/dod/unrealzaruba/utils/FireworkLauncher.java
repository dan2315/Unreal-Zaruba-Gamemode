package com.dod.unrealzaruba.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        CompoundTag fireworkTag = fireworkStack.getOrCreateTagElement("Fireworks");
        ListTag explosions = new ListTag();

        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) RANDOM.nextInt(5)); // Random shape
        explosion.putIntArray("Colors", new int[]{RANDOM.nextInt(0xFFFFFF)}); // Random color
        explosions.add(explosion);

        fireworkTag.putByte("Flight", (byte) 1); // Flight duration
        fireworkTag.put("Explosions", explosions);
        fireworkStack.addTagElement("Fireworks", fireworkTag);

        FireworkRocketEntity fireworkEntity = new FireworkRocketEntity(world, pos.getX(), pos.getY(), pos.getZ(), fireworkStack);
        world.addFreshEntity(fireworkEntity);
    }
}
