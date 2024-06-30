package com.dod.UnrealZaruba.SoundHandler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ObjectHolder;

@Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModSounds {
    @ObjectHolder("unrealzaruba:horn_radiant")
    public static final SoundEvent horn_radiant = null;

    @ObjectHolder("unrealzaruba:horn_dire")
    public static final SoundEvent horn_dire = null;

//    @ObjectHolder("unrealzaruba:ten_remaining")
//    public static final SoundEvent ten_remaining = null;

    @SubscribeEvent
    public static void registerSounds(final RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(new SoundEvent(new ResourceLocation("unrealzaruba", "horn_radiant")).setRegistryName("horn_radiant"));
        event.getRegistry().register(new SoundEvent(new ResourceLocation("unrealzaruba", "horn_dire")).setRegistryName("horn_dire"));
    }
}
