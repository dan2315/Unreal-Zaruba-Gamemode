package com.dod.unrealzaruba.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;

public class Conversion {
    public static byte fromNormalizedFloatToByte(float value) {
        float clamped = Math.max(0, Math.min(1, value));
        int scaled = Math.round(clamped * 255f) - 128;
        return (byte) scaled;
    }

    public static short fromNormalizedFloatToShort(float value) {
        float clamped = Math.max(0, Math.min(1, value));
        int scaled = Math.round(clamped * 65535f) - 32768;
        return (short) scaled;
    }

    public static Vec3 Vec3iToVec3(Vec3i position) {
        return new Vec3(position.getX(), position.getY(), position.getZ());
    }

    public static Vec3 BlockPosToVec3(BlockPos position) {
        return new Vec3(position.getX(), position.getY(), position.getZ());
    }

    public static BlockPos Vec3dcToBlockPos(Vector3dc positionInWorld) {
        return new BlockPos((int) positionInWorld.x(), (int) positionInWorld.y(), (int) positionInWorld.z());
    }
}
