package com.dod.UnrealZaruba;

import org.slf4j.Logger;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColorArgument;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.ModItems.ModItems;
import com.dod.UnrealZaruba.NetworkPackets.LoginPacket;
import com.dod.UnrealZaruba.RespawnCooldown.PlayerRespawnEventHandler;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.mojang.logging.LogUtils;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod("unrealzaruba")
public class UnrealZaruba {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("unrealzaruba", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "unrealzaruba";

    public UnrealZaruba() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new PlayerRespawnEventHandler());

        TeamColorArgument.RegisterArgument();
        ModSounds.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModBlocks.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModItems.register(FMLJavaModLoadingContext.get().getModEventBus());

        CHANNEL.registerMessage(0 ,LoginPacket.class,
                (msg, buf) -> LoginPacket.encode(msg, buf),
                (msg) -> LoginPacket.decode(msg),
                (msg, ctx) -> LoginPacket.handle(msg, ctx));
    }


    // public static void CommonSetup(FMLCommonSetupEvent event) {
    //     @SuppressWarnings("unchecked")
    //     Registry<DimensionType> dimensionTypeRegistry = (Registry<DimensionType>) Registry.REGISTRY.get(Registry.DIMENSION_TYPE_REGISTRY);
    //     if (dimensionTypeRegistry == null) {
    //         throw new IllegalStateException("DimensionType registry not found!");
    //     }
    //     WorldManager.SetupDimensionType(dimensionTypeRegistry);
    // }

}