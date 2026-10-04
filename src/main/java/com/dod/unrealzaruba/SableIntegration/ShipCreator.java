package com.dod.unrealzaruba.SableIntegration;

import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.SchematicLoader;

import com.dod.unrealzaruba.Vehicles.Vehicle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.function.Consumer;

public class ShipCreator {

    public static boolean CreateShipFromTemplate(ResourceLocation schematicLocation, BlockPos position, Direction direction, ServerLevel level, ServerPlayer player) {
        return CreateShipFromTemplate(schematicLocation, position, direction, level, player, null);
    }

    /**
     * Pastes a vehicle structure template into the world and assembles it into a Sable sub-level.
     */
    public static boolean CreateShipFromTemplate(ResourceLocation schematicLocation, BlockPos position, Direction direction, ServerLevel level, ServerPlayer player, Consumer<Vehicle> init) {
        try {
            String vehicleType = schematicLocation.getPath();
            StructureTemplate template = SchematicLoader.GetVehicleTemplate(schematicLocation, level);
            if (template == null) {
                UnrealZaruba.LOGGER.error("Vehicle template not found: {}", schematicLocation);
                return false;
            }

            Vec3i size = template.getSize();
            BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setRotation(rotationFor(direction))
                    .setRotationPivot(pivot)
                    .setIgnoreEntities(false);

            // Center the vehicle on the spawn position, one block above it
            BlockPos origin = position.above().subtract(pivot);
            List<BlockPos> blocks = SchematicLoader.GetPlacedBlockPositions(template, settings, origin, level);
            if (!template.placeInWorld(level, origin, origin, settings, level.random, Block.UPDATE_CLIENTS)) {
                UnrealZaruba.LOGGER.error("Failed to place vehicle template {}", schematicLocation);
                return false;
            }

            ServerSubLevel subLevel = SableShips.assemble(level, blocks);
            if (subLevel == null) {
                UnrealZaruba.LOGGER.error("Failed to assemble vehicle {} into a sub-level", schematicLocation);
                return false;
            }
            subLevel.setName(vehicleType);

            var ownedTeam = player == null ? null : PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext teamPlayerContext ? teamPlayerContext.Team().Color() : null;
            UnrealZaruba.vehicleManager.addVehicle(new Vehicle(vehicleType, ownedTeam, level, List.of(subLevel.getUniqueId()), blocks.size(), init));
            return true;
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("Error creating ship from template: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static Rotation rotationFor(Direction direction) {
        return switch (direction) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
