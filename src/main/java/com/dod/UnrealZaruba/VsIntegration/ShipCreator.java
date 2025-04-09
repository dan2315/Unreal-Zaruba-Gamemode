package com.dod.UnrealZaruba.VsIntegration;

import com.dod.UnrealZaruba.Utils.SchematicLoader;
import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.world.LevelYRange;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Blocks;

public class ShipCreator {

    private static final ResourceLocation WHEEL_BLOCK_ID = new ResourceLocation("trackwork:med_simple_wheel");
    private static final Block WHEEL_BLOCK = ForgeRegistries.BLOCKS.getValue(WHEEL_BLOCK_ID);

    private static final ResourceLocation REDSTONE_LINK_ID = new ResourceLocation("create:redstone_link");
    private static final Block REDSTONE_LINK_BLOCK = ForgeRegistries.BLOCKS.getValue(REDSTONE_LINK_ID);

    private static final ResourceLocation LECTERN_CONTROLLER_ID = new ResourceLocation("create:lectern_controller");
    private static final Block LECTERN_CONTROLLER_BLOCK = ForgeRegistries.BLOCKS.getValue(LECTERN_CONTROLLER_ID);


    public static void CreateShipFromTemplate(BlockPos position, ResourceLocation schematicLocation, ServerLevel level, Direction direction) {
        var server =  ServerLifecycleHooks.getCurrentServer();
        ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(server);
        BlockPos offsetedPosition = position.relative(direction, 3);

        var ship = shipWorld.createNewShipAtBlock(
            new Vector3i(offsetedPosition.getX(), offsetedPosition.getY(), offsetedPosition.getZ()),
            false,
            1.0,
            VSGameUtilsKt.getDimensionId(level));

        UnrealZaruba.LOGGER.info("[UnrealZaruba] Loading schematic into ship {}", schematicLocation);
        LoadSchematicIntoShip(schematicLocation, ship, level);
    }

    private static void LoadSchematicIntoShip(ResourceLocation schematicLocation, ServerShip ship, ServerLevel level) {
        StructureTemplate template = SchematicLoader.GetStructureTemplate(schematicLocation);
        Vector3i centerPos = new Vector3i();

        LevelYRange yRange = new LevelYRange(level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        ship.getChunkClaim().getCenterBlockCoordinates(yRange, centerPos);

        Vec3i templateSize = template.getSize();
        BlockPos shipPos = new BlockPos(centerPos.x, centerPos.y, centerPos.z);

        BlockPos placementPos = new BlockPos(-templateSize.getX()/2, 0, -templateSize.getZ()/2);;

        UnrealZaruba.LOGGER.info("Placing structure at position: x={}, y={}, z={}", 
        shipPos.getX(), shipPos.getY(), shipPos.getZ());

        template.placeInWorld(level, shipPos, shipPos.offset(placementPos), new StructurePlaceSettings(), level.random, Block.UPDATE_ALL);
        PostprocessBlocks(level, new BlockPos(templateSize), shipPos);
    }

    
    private static void PostprocessBlocks(ServerLevel level, BlockPos templateSize, BlockPos shipPos) {
        var server = ServerLifecycleHooks.getCurrentServer();

        List<BlockPos> wheelPositions = new ArrayList<>();

        for (int x = 0; x < templateSize.getX(); x++) {
            for (int y = 0; y < templateSize.getY(); y++) {
                for (int z = 0; z < templateSize.getZ(); z++) {
                    BlockPos pos = shipPos.offset(x, y, z);
                    BlockState initialState = level.getBlockState(pos);
                    String blockId = ForgeRegistries.BLOCKS.getKey(initialState.getBlock()).toString();
                    if (blockId.equals(WHEEL_BLOCK_ID.toString())) {
                        UnrealZaruba.LOGGER.info("Replacing wheel at position: x={}, y={}, z={}", pos.getX(), pos.getY(), pos.getZ());

                        String blockStateStr = initialState.toString();
                        int propertiesStart = blockStateStr.indexOf('[');
                        String propertiesPart = "";
                        if (propertiesStart > 0) {
                            propertiesPart = blockStateStr.substring(propertiesStart);
                        }
                        
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

                        String command = String.format("setblock %d %d %d %s%s", 
                            pos.getX(), pos.getY(), pos.getZ(), WHEEL_BLOCK_ID.toString(), propertiesPart);

                        server.getCommands().performPrefixedCommand(
                            server.createCommandSourceStack().withSuppressedOutput(), command);
                    }
                }
            }
        }

        ProcessWheels(level, wheelPositions);
    }

    private static void ProcessWheels(ServerLevel level, List<BlockPos> wheelPositions) {
        for (BlockPos pos : wheelPositions) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
