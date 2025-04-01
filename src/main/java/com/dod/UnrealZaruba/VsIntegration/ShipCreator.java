package com.dod.UnrealZaruba.VsIntegration;

import com.dod.UnrealZaruba.Utils.SchematicLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.world.LevelYRange;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class ShipCreator {
    public static void CreateShipFromTemplate(Vector3ic position, ResourceLocation schematicLocation, ServerLevel level) {

        var server =  ServerLifecycleHooks.getCurrentServer();
        ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(server);

        var ship = shipWorld.createNewShipAtBlock(position, false, 1.0, level.dimension().location().toString());

        BlockPos originPos = new BlockPos(position.x(), position.y(), position.z());

        LoadSchematicIntoShip(schematicLocation, ship, level, originPos);
    }

    private static void LoadSchematicIntoShip(ResourceLocation schematicLocation, ServerShip ship, ServerLevel level, BlockPos origin) {
        StructureTemplate template = SchematicLoader.GetStructureTemplate(schematicLocation);
        Vector3i centerPos = new Vector3i();
        LevelYRange yRange = new LevelYRange(level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        ship.getChunkClaim().getCenterBlockCoordinates(yRange, centerPos);

        var templateSize = template.getSize();
        BlockPos shipPos = new BlockPos(centerPos.x, centerPos.y, centerPos.z);

        BlockPos placementPos = new BlockPos(-templateSize.getX()/2, 0, -templateSize.getZ()/2);;

        template.placeInWorld(level, placementPos, shipPos, new StructurePlaceSettings(), level.random, Block.UPDATE_ALL);

    }

}
