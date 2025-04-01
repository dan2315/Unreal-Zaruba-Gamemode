package com.dod.UnrealZaruba.ModBlocks.ShipSpecificRedstoneLink;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

public class ShipSpecificLinkBehaviour extends LinkBehaviour {

    Integer shipID;

    protected ShipSpecificLinkBehaviour(SmartBlockEntity be, Pair<ValueBoxTransform, ValueBoxTransform> slots) {
        super(be, slots);
    }
    
}
