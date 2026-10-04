package com.dod.unrealzaruba.ModItems;

import javax.annotation.Nonnull;

import com.dod.unrealzaruba.SableIntegration.SableShips;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Debug tool: assembles the clicked block and everything connected to it into a Sable sub-level.
 */
public class HandAssembler extends Item {
    private static final int MAX_BLOCKS = 4096;

    public HandAssembler(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(@Nonnull UseOnContext useContext) {
        if (!(useContext.getLevel() instanceof ServerLevel level))
            return InteractionResult.FAIL;
        BlockPos blockPos = useContext.getClickedPos();

        var subLevel = SableShips.assembleConnected(level, blockPos, MAX_BLOCKS);
        if (subLevel == null) {
            UnrealZaruba.LOGGER.warn("Hand assembler failed to assemble at {}", blockPos);
            return InteractionResult.FAIL;
        }

        var position = subLevel.logicalPose().position();
        UnrealZaruba.LOGGER.info("Assembled sub-level {} at {}, {}, {}", subLevel.getUniqueId(), position.x(), position.y(), position.z());
        return InteractionResult.SUCCESS;
    }
}
