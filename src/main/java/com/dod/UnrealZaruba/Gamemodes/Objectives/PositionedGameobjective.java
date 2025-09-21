package com.dod.UnrealZaruba.Gamemodes.Objectives;

import net.minecraft.core.BlockPos;

public class PositionedGameobjective extends GameObjective {
    private final BlockPos position;

    public PositionedGameobjective(BlockPos position) {
        super();
        this.position = position;
    }
    
    public PositionedGameobjective(String name, String type, BlockPos position) {
        super(name, type);
        this.position = position;
    }

    public BlockPos GetPosition() {
        return position;
    }

    @Override
    protected boolean UpdateImplementation() {
        return false;
    }

    @Override
    protected void OnCompleted() {
    }
}
