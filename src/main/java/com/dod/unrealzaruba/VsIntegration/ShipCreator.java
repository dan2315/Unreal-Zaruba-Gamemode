package com.dod.unrealzaruba.VsIntegration;

import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.SchematicLoader;
import com.dod.unrealzaruba.utils.Geometry.Utils;

import java.util.UUID;

import com.dod.unrealzaruba.Vehicles.Vehicle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1;
import net.spaceeye.vmod.schematic.PasteSchematicSettings;
import org.joml.Vector3d;
import org.joml.Quaterniond;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.valkyrienskies.core.api.ships.ServerShip;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;
import kotlin.Unit;

import net.minecraft.server.level.ServerPlayer;

import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic;
import net.spaceeye.vmod.schematic.VModShipSchematicV2Kt;

public class ShipCreator {

    private static final ResourceLocation WHEEL_BLOCK_ID = new ResourceLocation("trackwork:med_simple_wheel");
    private static final Block WHEEL_BLOCK = ForgeRegistries.BLOCKS.getValue(WHEEL_BLOCK_ID);

    private static final ResourceLocation REDSTONE_LINK_ID = new ResourceLocation("create:redstone_link");
    private static final Block REDSTONE_LINK_BLOCK = ForgeRegistries.BLOCKS.getValue(REDSTONE_LINK_ID);

    private static final ResourceLocation LECTERN_CONTROLLER_ID = new ResourceLocation("create:lectern_controller");
    private static final Block LECTERN_CONTROLLER_BLOCK = ForgeRegistries.BLOCKS.getValue(LECTERN_CONTROLLER_ID);

    private static final ResourceLocation OAK_SHIP_HELM_ID = new ResourceLocation("vs_eureka:oak_ship_helm");

    private static final MinecraftServer SERVER = ServerLifecycleHooks.getCurrentServer();

    public static boolean CreateShipFromTemplate(ResourceLocation schematicLocation, BlockPos position, Direction direction, ServerLevel level, ServerPlayer player) {
        return CreateShipFromTemplate(schematicLocation, position, direction, level, player, null);
    }

    public static boolean CreateShipFromTemplate(ResourceLocation schematicLocation, BlockPos position, Direction direction, ServerLevel level, ServerPlayer player, Consumer<Vehicle> init) {
        try {
            String vehicleType = schematicLocation.getPath();
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

            List<ServerShip> serverShips = new ArrayList<>();
            PasteSchematicSettings settings = new PasteSchematicSettings();
            VModShipSchematicV2Kt.placeAt(schematicV1, level, player, player != null ? player.getUUID() : UUID.randomUUID(), positionVec, rotation, settings, ships -> {
                serverShips.addAll(ships);
                return Unit.INSTANCE;
            });
            var ownedTeam = player == null ? null : PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext teamPlayerContext ? teamPlayerContext.Team().Color() : null;
            UnrealZaruba.vehicleManager.addVehicle(new Vehicle(vehicleType, ownedTeam, serverShips, schematicV1, init));
            return true;
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("Error creating ship from template: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}