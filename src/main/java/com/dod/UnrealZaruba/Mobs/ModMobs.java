package com.dod.UnrealZaruba.Mobs;

import com.dod.UnrealZaruba.UnrealZaruba;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMobs {
    public static final DeferredRegister<EntityType<?>>ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, UnrealZaruba.MOD_ID);

    public static final RegistryObject<EntityType<ClickableHumanoidEntity>> CLICKABLE_HUMANOID_ENTITY = ENTITY_TYPES.register("clickable_humanoid_entity",
     () -> EntityType.Builder.of(ClickableHumanoidEntity::new, MobCategory.CREATURE)
    .sized(0.6f, 1.95f)  
    .build("custom_npc"));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
