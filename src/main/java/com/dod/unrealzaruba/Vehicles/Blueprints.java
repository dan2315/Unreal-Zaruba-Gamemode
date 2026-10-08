package com.dod.unrealzaruba.Vehicles;

import java.io.IOException;

import javax.annotation.Nullable;

import com.enxv.aeronauticsstructuretool.PlacementSnapMode;
import com.enxv.aeronauticsstructuretool.PortableStructurePreviewData;
import com.enxv.aeronauticsstructuretool.blueprint.placement.BlueprintVerticalPlacement;
import com.enxv.aeronauticsstructuretool.blueprint.placement.CreatePhysicalBlueprintService;
import com.enxv.aeronauticsstructuretool.blueprint.placement.NativeBlueprintPlacementService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    /** Null if the toolgun can't read the data. */
    @Nullable
    public static Info read(String name, byte[] data, @Nullable Level level) {
        try {
            PortableStructurePreviewData preview = PortableStructurePreviewData.fromBlueprintBytes(name, data, level);
            return new Info(preview.width(), preview.height(), preview.depth(), preview.blockCount(), preview.substructureCount());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Places a blueprint centered on (centerX, centerZ) with its lowest blocks' centers at minimumBlockCenterY.
     * Throws with the toolgun's reason when it refuses the blueprint or the spot.
     */
    public static void place(ServerPlayer player, ServerLevel level, String name, byte[] data,
                             double centerX, double centerZ, double minimumBlockCenterY) throws IOException {
        if (CreatePhysicalBlueprintService.hasCreatePhysicalLayout(data)) {
            CreatePhysicalBlueprintService.placeAlignedToMinimum(player, level, name, data, centerX, centerZ, minimumBlockCenterY);
            return;
        }
        double bottomY = PortableStructurePreviewData.fromBlueprintBytes(name, data, level).bottomY();
        BlockPos anchor = BlockPos.containing(centerX, minimumBlockCenterY - 1, centerZ);
        NativeBlueprintPlacementService.place(
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
    }
}
