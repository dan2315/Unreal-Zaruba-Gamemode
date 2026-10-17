package com.dod.unrealzaruba.Vehicles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

import com.enxv.aeronauticsstructuretool.PlacementSnapMode;
import com.enxv.aeronauticsstructuretool.PortableStructurePreviewData;
import com.enxv.aeronauticsstructuretool.blueprint.placement.BlueprintVerticalPlacement;
import com.enxv.aeronauticsstructuretool.blueprint.placement.CreatePhysicalBlueprintService;
import com.enxv.aeronauticsstructuretool.blueprint.placement.NativeBlueprintPlacementService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Create Aeronautics Toolgun blueprints (.excraft): the vehicles players share through the schematics browser.
 * Reading and placing go through the toolgun's own code, the same calls its portable printer makes.
 */
public final class Blueprints {
    private Blueprints() {}

    /** Size in blocks, total block count and number of physical sub-structures. */
    public record Info(int width, int height, int depth, int blocks, int sublevels) {
        public int maxSide() {
            return Math.max(width, Math.max(height, depth));
        }
    }

    /** Where the blocks of a blueprint lie in the horizontal plane, relative to the root of the vehicle (the point it is placed by). */
    public record Footprint(double minX, double maxX, double minZ, double maxZ) {
        public double centerX() { return (minX + maxX) / 2; }
        public double centerZ() { return (minZ + maxZ) / 2; }
        public double sizeX() { return maxX - minX; }
        public double sizeZ() { return maxZ - minZ; }
    }

    private static final long MAX_UNPACKED_BYTES = 32L * 1024 * 1024;

    /**
     * The footprint of the blueprint as it is placed (upright, aligned to the axes), worked out the way the hologram is,
     * so that what the player sees is what the vehicle does. Null when it cannot be worked out.
     */
    @Nullable
    public static Footprint footprint(byte[] data, @Nullable Level level) {
        try {
            var preview = com.enxv.aeronauticsstructuretool.PreviewBlueprintData.parse(withoutSavedRotation(data), level);
            double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
            // The blocks with models; when there are none, the outline points stand in for them
            for (int pass = 0; pass < 2 && minX == Double.POSITIVE_INFINITY; pass++) {
                for (var sub : preview.subLevels()) {
                    org.joml.Vector3d offset = new org.joml.Vector3d(sub.relativePosition()).sub(preview.rootRelativePosition());
                    org.joml.Quaterniond rotation = new org.joml.Quaterniond(sub.relativeOrientation());
                    java.util.List<org.joml.Vector3d> centers = new java.util.ArrayList<>();
                    if (pass == 0) {
                        for (var block : sub.previewBlocks()) centers.add(new org.joml.Vector3d(block.center()));
                    } else {
                        for (org.joml.Vector3d point : sub.sampleBlocks()) centers.add(new org.joml.Vector3d(point));
                    }
                    for (org.joml.Vector3d center : centers) {
                        org.joml.Vector3d local = new org.joml.Vector3d(center).sub(sub.localPlotAnchor());
                        rotation.transform(local);
                        local.add(offset);
                        minX = Math.min(minX, local.x - 0.5);
                        maxX = Math.max(maxX, local.x + 0.5);
                        minZ = Math.min(minZ, local.z - 0.5);
                        maxZ = Math.max(maxZ, local.z + 0.5);
                    }
                }
            }
            return minX == Double.POSITIVE_INFINITY ? null : new Footprint(minX, maxX, minZ, maxZ);
        } catch (IOException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /**
     * Blueprints remember the world orientation their vehicle had when it was saved (root_orientation) and the toolgun
     * places and measures them with it, so the same car comes out turned and tilted differently each time. We always
     * want the vehicle upright and aligned to the axes, so the saved rotation is replaced with the identity before the
     * blueprint is measured or placed. Anything that isn't a native blueprint, or can't be parsed, is returned as is
     * and left to the toolgun to accept or refuse.
     */
    public static byte[] withoutSavedRotation(byte[] data) {
        try {
            CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.create(MAX_UNPACKED_BYTES));
            if (!root.contains("root_orientation", Tag.TAG_COMPOUND)) return data;
            CompoundTag identity = new CompoundTag();
            identity.putDouble("w", 1.0);
            identity.putDouble("x", 0.0);
            identity.putDouble("y", 0.0);
            identity.putDouble("z", 0.0);
            root.put("root_orientation", identity);
            ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
            NbtIo.writeCompressed(root, out);
            return out.toByteArray();
        } catch (IOException | RuntimeException e) {
            return data;
        }
    }

    /** Null if the toolgun can't read the data. Measured as {@link #place} would place it: without the saved rotation. */
    @Nullable
    public static Info read(String name, byte[] data, @Nullable Level level) {
        try {
            PortableStructurePreviewData preview = PortableStructurePreviewData.fromBlueprintBytes(name, withoutSavedRotation(data), level);
            return new Info(preview.width(), preview.height(), preview.depth(), preview.blockCount(), preview.substructureCount());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Places a blueprint centered on (centerX, centerZ) with its lowest blocks' centers at minimumBlockCenterY.
     * Throws with the toolgun's reason when it refuses the blueprint or the spot. Returns the id of the vehicle's main
     * sub-level when the placement says which one it is, null otherwise.
     */
    @Nullable
    public static java.util.UUID place(ServerPlayer player, ServerLevel level, String name, byte[] data,
                             double centerX, double centerZ, double minimumBlockCenterY) throws IOException {
        data = withoutSavedRotation(data);
        if (CreatePhysicalBlueprintService.hasCreatePhysicalLayout(data)) {
            CreatePhysicalBlueprintService.placeAlignedToMinimum(player, level, name, data, centerX, centerZ, minimumBlockCenterY);
            return null;
        }
        double bottomY = PortableStructurePreviewData.fromBlueprintBytes(name, data, level).bottomY();
        BlockPos anchor = BlockPos.containing(centerX, minimumBlockCenterY - 1, centerZ);
        var result = NativeBlueprintPlacementService.place(
                level,
                anchor,
                Direction.UP,
                name,
                data,
                centerX,
                anchor.getY() + 1.0D,
                centerZ,
                0,
                100,
                0,
                0,
                0,
                PlacementSnapMode.HIT,
                null,
                BlueprintVerticalPlacement.alignMinimumCenter(minimumBlockCenterY, bottomY),
                player.getUUID(),
                null
        );
        return result != null && result.rootSubLevel() != null ? result.rootSubLevel().getUniqueId() : null;
    }
}
