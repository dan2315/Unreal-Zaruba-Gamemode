package com.dod.UnrealZaruba.SoundHandler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * DeferredRegister звуков
 */
public class ModSounds {
    private static final String MOD_ID = "unrealzaruba";

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);

    public static final RegistryObject<SoundEvent> HORN_DIRE = registerSound("horn_dire");
    public static final RegistryObject<SoundEvent> HORN_RADIANT = registerSound("horn_radiant");
    public static final RegistryObject<SoundEvent> DEATH = registerSound("death");
    public static final RegistryObject<SoundEvent> RESPAWN = registerSound("respawn");
    public static final RegistryObject<SoundEvent> RESPAWN2 = registerSound("respawn2");
    public static final RegistryObject<SoundEvent> SELECT1 = registerSound("select1");
    public static final RegistryObject<SoundEvent> SELECT2 = registerSound("select2");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
