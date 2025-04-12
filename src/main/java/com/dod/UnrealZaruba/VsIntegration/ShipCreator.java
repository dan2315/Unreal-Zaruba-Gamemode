package com.dod.UnrealZaruba.VsIntegration;

import com.dod.UnrealZaruba.Utils.SchematicLoader;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.Geometry.Utils;
import com.simibubi.create.foundation.utility.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;

import kotlin.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic;
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1;
import net.spaceeye.vmod.schematic.VModShipSchematicV1Kt;

public class ShipCreator {

    private static final ResourceLocation WHEEL_BLOCK_ID = new ResourceLocation("trackwork:med_simple_wheel");
    private static final Block WHEEL_BLOCK = ForgeRegistries.BLOCKS.getValue(WHEEL_BLOCK_ID);

    private static final ResourceLocation REDSTONE_LINK_ID = new ResourceLocation("create:redstone_link");
    private static final Block REDSTONE_LINK_BLOCK = ForgeRegistries.BLOCKS.getValue(REDSTONE_LINK_ID);

    private static final ResourceLocation LECTERN_CONTROLLER_ID = new ResourceLocation("create:lectern_controller");
    private static final Block LECTERN_CONTROLLER_BLOCK = ForgeRegistries.BLOCKS.getValue(LECTERN_CONTROLLER_ID);

    public static boolean CreateShipFromTemplate(BlockPos position, ResourceLocation schematicLocation, ServerLevel level,
            ServerPlayer player, Direction direction) {
        try {
            IShipSchematic schematic = SchematicLoader.GetVSchem(schematicLocation);
            IShipSchematicDataV1 schematicV1 = (IShipSchematicDataV1) schematic;

            UnrealZaruba.LOGGER.info("Got schematic: ");
            schematic.getInfo().getShipsInfo().forEach(shipData -> {
                UnrealZaruba.LOGGER.info("Ship data: " + shipData.getId());
            });
            Quaterniond rotation = Utils.getQuatFromDir(direction);
            BlockPos offsetedPosition = position.relative(direction);
            Vector3d positionVec = new Vector3d(offsetedPosition.getX(), offsetedPosition.getY(), offsetedPosition.getZ());
            VModShipSchematicV1Kt.placeAt(schematicV1, level, player, player.getUUID(),
                    positionVec, rotation,
                    ships -> Unit.INSTANCE);
            return true;
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("Error creating ship from template: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean PlaceAt(IShipSchematic schematic, ServerLevel level, ServerPlayer player, UUID uuid,
            Vector3d position, Quaterniondc rotation, Consumer<List<ServerShip>> postCreationCallback) {
        var newTransforms = new ArrayList<ShipTransform>();
        var shipConstructors = CreateShipConstructors(schematic, level, position, rotation, newTransforms);

        // RIP
        return false;
    }

    private static List<Pair<Supplier<ServerShip>, Long>> CreateShipConstructors(
            IShipSchematic schematic,
            ServerLevel level,
            Vector3d position,
            Quaterniondc rotation,
            List<ShipTransform> newTransforms) {
        ShipTransform center = ShipTransformImpl.Companion.create(
                new Vector3d(),
                new Vector3d(),
                new Quaterniond(),
                new Vector3d(1.0, 1.0, 1.0));

        List<Pair<Supplier<ServerShip>, Long>> shipConstructors = new ArrayList<>();
        schematic.getInfo().getShipsInfo().forEach(shipData -> {
            ShipTransform shipTransform = ShipTransformImpl.Companion.create(
                    shipData.getRelPositionToCenter(),
                    shipData.getPositionInShip(),
                    shipData.getRotation(),
                    new Vector3d(shipData.getShipScale(), shipData.getShipScale(), shipData.getShipScale()));
            ShipTransform newTransform = Utils.RotateAroundCenter(center, shipTransform, rotation);
            newTransforms.add(newTransform);
            var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);

            var destinationPosition = position.add(newTransform.getPositionInWorld());

            ServerShip ship = shipWorld.createNewShipAtBlock(
                    new Vector3i(),
                    false,
                    shipData.getShipScale(),
                    VSGameUtilsKt.getDimensionId(level));

            ship.setStatic(true);

            shipWorld.teleportShip(ship, new ShipTeleportDataImpl(
                    destinationPosition,
                    newTransform.getShipToWorldRotation(),
                    new Vector3d(0, 0, 0),
                    newTransform.getShipToWorldScaling()));
        });

        return shipConstructors;
    }
}