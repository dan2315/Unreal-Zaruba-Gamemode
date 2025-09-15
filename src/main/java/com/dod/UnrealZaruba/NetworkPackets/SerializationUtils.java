package com.dod.UnrealZaruba.NetworkPackets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;

public class SerializationUtils {
    public static void encodeAABB(FriendlyByteBuf buf, AABB box) {
        buf.writeDouble(box.minX);
        buf.writeDouble(box.minY);
        buf.writeDouble(box.minZ);
        buf.writeDouble(box.maxX);
        buf.writeDouble(box.maxY);
        buf.writeDouble(box.maxZ);
    }

    public static AABB decodeAABB(FriendlyByteBuf buf) {
        double minX = buf.readDouble();
        double minY = buf.readDouble();
        double minZ = buf.readDouble();
        double maxX = buf.readDouble();
        double maxY = buf.readDouble();
        double maxZ = buf.readDouble();
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

}
