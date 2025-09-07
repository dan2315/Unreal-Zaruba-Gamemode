package com.dod.UnrealZaruba.Mobs;

import com.dod.UnrealZaruba.UnrealZaruba;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class AttributesRegistration {

    @SubscribeEvent
    public static void onRegisterAttributes(EntityAttributeCreationEvent event) {
        UnrealZaruba.LOGGER.warn("Attributes registered!");
        event.put(ModMobs.CLICKABLE_HUMANOID_ENTITY.get(), 
            AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH, 9999)
            .add(Attributes.MOVEMENT_SPEED, 0)
            .add(Attributes.FOLLOW_RANGE, 0)
            .add(Attributes.ARMOR)
            .add(Attributes.ARMOR_TOUGHNESS)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(ForgeMod.ENTITY_GRAVITY.get())
            .build());
    }
}
