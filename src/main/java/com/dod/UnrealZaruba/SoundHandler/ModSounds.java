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

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}