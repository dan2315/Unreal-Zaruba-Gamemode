package com.dod.unrealzaruba.SableIntegration;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * All direct calls into Sable (the physics engine behind Create Aeronautics) live here.
 * Replaces the old Valkyrien Skies ship API usage.
 */
public class SableShips {

    /**
     * Turns already placed world blocks into a physics sub-level.
     */
    @Nullable
    public static ServerSubLevel assemble(ServerLevel level, Collection<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return null;
        }
        BoundingBox3i bounds = BoundingBox3i.from(blocks);
        if (bounds == null) {
            return null;
        }
        // Same padding Sable's own /assemble command uses, so nearby entities and tracking points move along
        bounds.set(bounds.minX - 1, bounds.minY - 1, bounds.minZ - 1, bounds.maxX + 1, bounds.maxY + 1, bounds.maxZ + 1);

        BlockPos anchor = closestTo(blocks, (bounds.minX + bounds.maxX) / 2, (bounds.minY + bounds.maxY) / 2, (bounds.minZ + bounds.maxZ) / 2);
        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, bounds);
        if (subLevel.getMassTracker().isInvalid()) {
            remove(level, subLevel.getUniqueId());
            return null;
        }
        return subLevel;
    }

    /**
     * Gathers every block connected to {@code origin} and assembles them into a sub-level.
     */
    @Nullable
    public static ServerSubLevel assembleConnected(ServerLevel level, BlockPos origin, int maxBlocks) {
        var result = SubLevelAssemblyHelper.gatherConnectedBlocks(origin, level, maxBlocks, null);
        if (result.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS || result.blocks() == null) {
            return null;
        }
        return assemble(level, result.blocks());
    }

    @Nullable
    public static ServerSubLevel get(ServerLevel level, UUID id) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }
        return container.getSubLevel(id) instanceof ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved() ? serverSubLevel : null;
    }

    /**
     * The sub-level whose plot contains this (plot-space) block position, if any.
     */
    @Nullable
    public static SubLevel getContaining(ServerLevel level, BlockPos pos) {
        return Sable.HELPER.getContaining(level, pos);
    }

    public static void remove(ServerLevel level, UUID id) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        SubLevel subLevel = container.getSubLevel(id);
        if (subLevel != null && !subLevel.isRemoved()) {
            container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
        }
    }

    public static void removeAll(ServerLevel level) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        for (ServerSubLevel subLevel : new ArrayList<>(container.getAllSubLevels())) {
            if (!subLevel.isRemoved()) {
                container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
            }
        }
    }

    private static BlockPos closestTo(Collection<BlockPos> blocks, int x, int y, int z) {
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        for (BlockPos pos : blocks) {
            long dx = pos.getX() - x, dy = pos.getY() - y, dz = pos.getZ() - z;
            long distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }
        return best;
    }
}
