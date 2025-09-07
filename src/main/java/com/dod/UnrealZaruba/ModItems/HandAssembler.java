package com.dod.UnrealZaruba.ModItems;

import javax.annotation.Nonnull;

import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;

import com.dod.UnrealZaruba.UnrealZaruba;

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
        if (!(level instanceof ServerLevel))
            return InteractionResult.FAIL;
        BlockPos blockPos = useContext.getClickedPos();
        var block = useContext.getLevel().getBlockState(blockPos);
        
        DenseBlockPosSet denseBlockPosSet = new DenseBlockPosSet();
        denseBlockPosSet.add(new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        ServerShip createdShip = ShipAssemblyKt.createNewShipWithBlocks(blockPos, denseBlockPosSet,
                (ServerLevel) useContext.getLevel());

        DecimalFormat df = new DecimalFormat("#");
        Vector3dc shipPos = createdShip.getTransform().getPositionInShip();
        UnrealZaruba.LOGGER.warn("Position In Ship X {}", df.format(createdShip.getTransform().getPositionInShip().x()));
        UnrealZaruba.LOGGER.warn("Position In Ship Y {}", df.format(createdShip.getTransform().getPositionInShip().y()));
        UnrealZaruba.LOGGER.warn("Position In Ship Z {}", df.format(createdShip.getTransform().getPositionInShip().z()));
        
        createdShip.setStatic(true);
        BlockState blockState = Blocks.STONE.defaultBlockState();
        level.setBlock(new BlockPos((int)shipPos.x(), level.getMinBuildHeight(), (int)shipPos.z()), blockState, 3);

        //ContraptionManager.loadSchematicToWorld((ServerLevel) level, new BlockPos((int)shipPos.x(), (int)shipPos.y(), (int)shipPos.z()), ContraptionManager.readSchematicFile("schematics\\sample.nbt"));

        return InteractionResult.SUCCESS;
    };
}