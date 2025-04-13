package com.dod.UnrealZaruba.VsIntegration;

import com.dod.UnrealZaruba.Utils.SchematicLoader;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.Geometry.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3d;
import org.joml.Quaterniond;

import kotlin.Unit;

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

            schematic.getInfo().getShipsInfo().forEach(shipData -> {
                UnrealZaruba.LOGGER.info("Ship data: " + shipData.getId());
            });
            Quaterniond rotation = Utils.getQuatFromDir(direction);
            BlockPos offsetedPosition = position.relative(direction, 3);
            Vector3d positionVec = new Vector3d( // TODO: Make this a function
                offsetedPosition.getX() + schematic.getInfo().getMaxObjectPos().x,
                offsetedPosition.getY() + schematic.getInfo().getMaxObjectPos().y,
                offsetedPosition.getZ() + schematic.getInfo().getMaxObjectPos().z);
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
}