package com.dod.UnrealZaruba;

import org.slf4j.Logger;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColorArgument;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.ModItems.ModItems;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.RespawnCooldown.PlayerRespawnEventHandler;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.mojang.logging.LogUtils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(UnrealZaruba.MOD_ID)
public class UnrealZaruba {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "unrealzaruba";

    public UnrealZaruba() {
        // Register mod event listeners
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new PlayerRespawnEventHandler());

        // Register other mod components
        TeamColorArgument.RegisterArgument();
        ModSounds.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModBlocks.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModItems.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Initialize network handler (register packets)
        NetworkHandler.init();
    }

    @SubscribeEvent
    public static void CommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("COMMON SETUP STAGE");
    }
}
