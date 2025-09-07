package com.dod.UnrealZaruba.VsIntegration;

import com.dod.UnrealZaruba.Utils.SchematicLoader;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.Geometry.Utils;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3d;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Quaterniond;
import java.util.List;
import java.util.ArrayList;
import org.valkyrienskies.core.api.ships.ServerShip;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import org.valkyrienskies.core.api.ships.PhysShip;
import kotlin.Unit;
import org.apache.commons.lang3.tuple.Pair;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.eureka.EurekaConfig;
import org.valkyrienskies.eureka.ship.EurekaShipControl;

import net.minecraft.server.level.ServerPlayer;

import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic;
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1;
import net.spaceeye.vmod.schematic.VModShipSchematicV1Kt;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class ShipCreator {

    private static final ResourceLocation WHEEL_BLOCK_ID = new ResourceLocation("trackwork:med_simple_wheel");
    private static final Block WHEEL_BLOCK = ForgeRegistries.BLOCKS.getValue(WHEEL_BLOCK_ID);

    private static final ResourceLocation REDSTONE_LINK_ID = new ResourceLocation("create:redstone_link");
    private static final Block REDSTONE_LINK_BLOCK = ForgeRegistries.BLOCKS.getValue(REDSTONE_LINK_ID);

    private static final ResourceLocation LECTERN_CONTROLLER_ID = new ResourceLocation("create:lectern_controller");
    private static final Block LECTERN_CONTROLLER_BLOCK = ForgeRegistries.BLOCKS.getValue(LECTERN_CONTROLLER_ID);

    private static final ResourceLocation OAK_SHIP_HELM_ID = new ResourceLocation("vs_eureka:oak_ship_helm");

    private static final MinecraftServer SERVER = ServerLifecycleHooks.getCurrentServer();

    public static Pair<Boolean, List<ServerShip>> CreateShipFromTemplate(BlockPos position, ResourceLocation schematicLocation, ServerLevel level, List<Runnable> delayedTasks) {
        try {
            IShipSchematic schematic = SchematicLoader.GetVSchem(schematicLocation);
            IShipSchematicDataV1 schematicV1 = (IShipSchematicDataV1) schematic;

            schematic.getInfo().getShipsInfo().forEach(shipData -> {
                UnrealZaruba.LOGGER.info("Ship data: " + shipData.getId());
            });
            Quaterniond rotation = new Quaterniond();
            Vector3d positionVec = new Vector3d(
                position.getX(),
                position.getY() + schematic.getInfo().getMaxObjectPos().y,
                position.getZ());
            List<ServerShip> serverShips = new ArrayList<>();
            VModShipSchematicV1Kt.placeAt(schematicV1, level, null, UUID.randomUUID(), positionVec, rotation, ships -> 
            {
                ships.forEach(ship -> {
                    var shipControl = EurekaShipControl.Companion.getOrCreate(ship);
                    shipControl.setPowerLinear(100);
                    shipControl.setFloaters(shipControl.getFloaters());
                    UnrealZaruba.LOGGER.info("Here is SHIP CONTROL floaters: {}", shipControl.getFloaters());
                    serverShips.add(ship);
                });
                return Unit.INSTANCE;
            });
            return Pair.of(true, serverShips);
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("Error creating ship from template: " + e.getMessage());
            e.printStackTrace();
            return Pair.of(false, new ArrayList<>());
        }
    }

    public static boolean CreateShipFromTemplate(BlockPos position, ResourceLocation schematicLocation, ServerLevel level,
            ServerPlayer player, Direction direction) {
        try {
            IShipSchematic schematic = SchematicLoader.GetVSchem(schematicLocation);
            IShipSchematicDataV1 schematicV1 = (IShipSchematicDataV1) schematic;

            schematic.getInfo().getShipsInfo().forEach(shipData -> {
                UnrealZaruba.LOGGER.info("Ship data: " + shipData.getId());
            });
            Quaterniond rotation = Utils.getQuatFromDir(direction);
            BlockPos offsetedPosition = position.relative(direction, -(int)schematic.getInfo().getMaxObjectPos().z);
            Vector3d positionVec = new Vector3d(
                offsetedPosition.getX(),
                offsetedPosition.getY() + schematic.getInfo().getMaxObjectPos().y,
                offsetedPosition.getZ());
            VModShipSchematicV1Kt.placeAt(schematicV1, level, player, player != null ? player.getUUID() : UUID.randomUUID(),
                    positionVec, rotation,
                    ships -> Unit.INSTANCE);
            return true;
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("Error creating ship from template: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}