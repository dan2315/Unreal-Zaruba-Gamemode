package com.dod.UnrealZaruba.ModBlocks;

import com.dod.UnrealZaruba.TeamLogic.TeamManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TeamBlock extends Block {
    private final DyeColor teamColor;

    public TeamBlock(DyeColor teamColor, Properties properties) {
        super(properties);
        this.teamColor = teamColor;
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState blockState, Entity entity) {
        if (!world.isClientSide && entity instanceof Player) {
            ServerPlayer player = (ServerPlayer) entity;
            if (player.isCrouching()) {
                TeamManager.AssignTo(teamColor, player);
            }
        }
    }
}
