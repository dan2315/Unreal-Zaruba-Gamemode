package com.dod.UnrealZaruba.Gamemodes.Objectives;

import net.minecraft.core.BlockPos;

public class PositionedGameobjective extends GameObjective {
    private final BlockPos position;

    public PositionedGameobjective(BlockPos position) {
        this.position = position;
    }

    public BlockPos getPosition() {
        return position;
    }

    @Override
    public Boolean IsCompleted() {
        return false;
    }

    @Override
    public void Update() {
    }


    
}
