package com.dod.UnrealZaruba;

import org.slf4j.Logger;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColorArgument;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.ModItems.ModItems;
import com.dod.UnrealZaruba.NetworkPackets.LoginPacket;
import com.dod.UnrealZaruba.NetworkPackets.OpenScreenPacket;
import com.dod.UnrealZaruba.NetworkPackets.SaveTokensPacket;
import com.dod.UnrealZaruba.NetworkPackets.VotePlayerPacket;
import com.dod.UnrealZaruba.RespawnCooldown.PlayerRespawnEventHandler;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

        CHANNEL.registerMessage(1 , SaveTokensPacket.class,
                (msg, buf) -> SaveTokensPacket.encode(msg, buf),
                (msg) -> SaveTokensPacket.decode(msg),
                (msg, ctx) -> SaveTokensPacket.handle(msg, ctx));

        CHANNEL.registerMessage(2 , OpenScreenPacket.class,
                (msg, buf) -> OpenScreenPacket.encode(msg, buf),
                (msg) -> OpenScreenPacket.decode(msg),
                (msg, ctx) -> OpenScreenPacket.handle(msg, ctx));

        CHANNEL.registerMessage(3 , VotePlayerPacket.class,
                (msg, buf) -> VotePlayerPacket.encode(msg, buf),
                (msg) -> VotePlayerPacket.decode(msg),
                (msg, ctx) -> VotePlayerPacket.handle(msg, ctx));
    }

    @SubscribeEvent
    public static void CommonSetup(FMLCommonSetupEvent event) {
        LOGGER.warn("[INFO] COMMON SETUP STAGE");
    }

}