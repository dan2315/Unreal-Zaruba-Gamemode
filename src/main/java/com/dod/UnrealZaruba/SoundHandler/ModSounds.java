package com.dod.UnrealZaruba.SoundHandler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "unrealzaruba");

    public static final RegistryObject<SoundEvent> HORN_DIRE = SOUND_EVENTS.register("horn_dire", () -> new SoundEvent(new ResourceLocation("unrealzaruba", "horn_dire")));
    public static final RegistryObject<SoundEvent> HORN_RADIANT = SOUND_EVENTS.register("horn_radiant", () -> new SoundEvent(new ResourceLocation("unrealzaruba", "horn_radiant")));
    public static final RegistryObject<SoundEvent> DEATH = SOUND_EVENTS.register("death", () -> new SoundEvent(new ResourceLocation("unrealzaruba", "death")));
    public static final RegistryObject<SoundEvent> RESPAWN = SOUND_EVENTS.register("respawn", () -> new SoundEvent(new ResourceLocation("unrealzaruba", "respawn")));
    public static final RegistryObject<SoundEvent> RESPAWN2 = SOUND_EVENTS.register("respawn2", () -> new SoundEvent(new ResourceLocation("unrealzaruba", "respawn2")));
    public static final RegistryObject<SoundEvent> SELECT1 = SOUND_EVENTS.register("select1", () -> new SoundEvent(new ResourceLocation("unrealzaruba", "select1")));
    public static final RegistryObject<SoundEvent> SELECT2 = SOUND_EVENTS.register("select2", () -> new SoundEvent(new ResourceLocation("unrealzaruba", "select2")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}