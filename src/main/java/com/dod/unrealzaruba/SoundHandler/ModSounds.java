package com.dod.unrealzaruba.SoundHandler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * DeferredRegister звуков
 */
public class ModSounds {
    private static final String MOD_ID = "unrealzaruba";

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_DIRE = registerSound("horn_dire");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_RADIANT = registerSound("horn_radiant");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH = registerSound("death");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESPAWN = registerSound("respawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESPAWN2 = registerSound("respawn2");
    public static final DeferredHolder<SoundEvent, SoundEvent> SELECT1 = registerSound("select1");
    public static final DeferredHolder<SoundEvent, SoundEvent> SELECT2 = registerSound("select2");

    private static final java.util.Map<VoiceLine, DeferredHolder<SoundEvent, SoundEvent>> VOICE = new java.util.EnumMap<>(VoiceLine.class);

    static {
        for (VoiceLine line : VoiceLine.values()) VOICE.put(line, registerSound(line.soundName()));
    }

    /** The sound event of a voice line. */
    public static DeferredHolder<SoundEvent, SoundEvent> voice(VoiceLine line) {
        return VOICE.get(line);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
