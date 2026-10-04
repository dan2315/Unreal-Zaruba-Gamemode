package com.dod.unrealzaruba.Mobs;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMobs {
    public static final DeferredRegister<EntityType<?>>ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, UnrealZaruba.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ClickableHumanoidEntity>> CLICKABLE_HUMANOID_ENTITY = ENTITY_TYPES.register("clickable_humanoid_entity",
     () -> EntityType.Builder.of(ClickableHumanoidEntity::new, MobCategory.CREATURE)
    .sized(0.6f, 1.95f)  
    .build("custom_npc"));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
