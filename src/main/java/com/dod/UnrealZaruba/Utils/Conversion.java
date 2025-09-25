package com.dod.unrealzaruba.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import xaero.hud.minimap.waypoint.WaypointColor;

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

    public static WaypointColor IntColorToXaeroColor(int color) {
        WaypointColor closest = null;
        double closestDistance = Double.MAX_VALUE;

        int r1 = (color >> 16) & 0xFF;
        int g1 = (color >> 8) & 0xFF;
        int b1 = color & 0xFF;

        for (WaypointColor wc : WaypointColor.values()) {
            int hex = wc.getHex();
            int r2 = (hex >> 16) & 0xFF;
            int g2 = (hex >> 8) & 0xFF;
            int b2 = hex & 0xFF;

            double distance = Math.sqrt(Math.pow(r1 - r2, 2) +
                    Math.pow(g1 - g2, 2) +
                    Math.pow(b1 - b2, 2));
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = wc;
            }
        }

        return closest;
    }

    public static BlockPos Vec3dcToBlockPos(Vector3dc positionInWorld) {
        return new BlockPos((int) positionInWorld.x(), (int) positionInWorld.y(), (int) positionInWorld.z());
    }
}
