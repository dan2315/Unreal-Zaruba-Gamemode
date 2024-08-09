package com.dod.UnrealZaruba.ModItems;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nonnull;

import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;

import com.dod.UnrealZaruba.unrealzaruba;
import com.dod.UnrealZaruba.ContraptionManager.ContraptionManager;
import com.ibm.icu.text.DecimalFormat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;


public class HandAssembler extends Item {



    public HandAssembler(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public InteractionResult useOn(@Nonnull UseOnContext useContext) {
        Level level = useContext.getLevel();
        if ((level instanceof ServerLevel) == false)
            return InteractionResult.FAIL;
        BlockPos blockPos = useContext.getClickedPos();
        var block = useContext.getLevel().getBlockState(blockPos);
        

        DenseBlockPosSet denseBlockPosSet = new DenseBlockPosSet();
        denseBlockPosSet.add(new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        ServerShip createdShip = ShipAssemblyKt.createNewShipWithBlocks(blockPos, denseBlockPosSet,
                (ServerLevel) useContext.getLevel());
        
        int shipChunkX = createdShip.getChunkClaim().getXMiddle();
        int shipChunkZ = createdShip.getChunkClaim().getZMiddle();

        DecimalFormat df = new DecimalFormat("#");
        Vector3dc shipPos = createdShip.getTransform().getPositionInShip();
        unrealzaruba.LOGGER.warn("Position In Ship X " + df.format(createdShip.getTransform().getPositionInShip().x()));
        unrealzaruba.LOGGER.warn("Position In Ship Y " + df.format(createdShip.getTransform().getPositionInShip().y()));
        unrealzaruba.LOGGER.warn("Position In Ship Z " + df.format(createdShip.getTransform().getPositionInShip().z()));
        
        createdShip.setStatic(true);
        BlockState blockState = Blocks.STONE.defaultBlockState();
        level.setBlock(new BlockPos(shipPos.x(), level.getMinBuildHeight(), shipPos.z()), blockState, 3);

        ContraptionManager.loadSchematicToWorld((ServerLevel) level, new BlockPos(shipPos.x(), shipPos.y(), shipPos.z()), ContraptionManager.readSchematicFile("schematics\\sample.nbt"));


        return InteractionResult.SUCCESS;
    };

}