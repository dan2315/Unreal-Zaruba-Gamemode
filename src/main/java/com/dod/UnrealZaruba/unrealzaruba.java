package com.dod.UnrealZaruba;

import com.dod.UnrealZaruba.Commands.CommandRegistration;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColorArgument;
import com.dod.UnrealZaruba.DiscordIntegration.CallbackServer;
import com.dod.UnrealZaruba.DiscordIntegration.DiscordAuth;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.DestroyObjectivesGamemode;
import com.dod.UnrealZaruba.Gamemodes.ScoreboardManager;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjectivesHandler;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.ModItems.ModItems;
import com.dod.UnrealZaruba.NetworkPackets.LoginPacket;
import com.dod.UnrealZaruba.RespawnCooldown.PlayerRespawnEventHandler;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.dod.UnrealZaruba.Utils.TimerManager;
import com.dod.UnrealZaruba.TeamLogic.TeamU;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

import org.slf4j.Logger;

@Mod("unrealzaruba")
public class unrealzaruba {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("unrealzaruba", "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "unrealzaruba";
    TimerManager timerManager = new TimerManager();

    public unrealzaruba() {
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

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        CallbackServer.StartServer();
        TeamU.SetupMinecraftTeams(event.getServer());
        new DestroyObjectivesGamemode();
        ScoreboardManager.clearScoreboard(event.getServer());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase == ServerTickEvent.Phase.START) {
            TimerManager.UpdateAll();
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        unrealzaruba.LOGGER.info("Server has stopped. Finalizing...");
        DestructibleObjectivesHandler.Save();
        BaseGamemode.currentGamemode.TeamManager.Save();
        CallbackServer.StopServer();
    }


    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (! ServerLifecycleHooks.getCurrentServer().isDedicatedServer()) return;
        BaseGamemode.currentGamemode.HandleNewPlayer(event.getPlayer());
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        String state = UUID.randomUUID().toString(); // Unique state to prevent CSRF
        DiscordAuth.unresolvedRequests.add(state);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new LoginPacket(state ,player.getUUID(), player.getName().getString()));
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (! ServerLifecycleHooks.getCurrentServer().isDedicatedServer()) return;
        CallbackServer.DeauthorizeUser(event.getPlayer().getUUID());
    }

    @Mod.EventBusSubscriber
    public static class CommandRegistryEvent {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            LOGGER.info("COMMANDS Registered");
            CommandRegistration.onCommandRegister(event);
        }
    }
}
