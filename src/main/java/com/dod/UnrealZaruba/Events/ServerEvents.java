package com.dod.UnrealZaruba.Events;

import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Commands.CommandRegistration;
import com.dod.UnrealZaruba.DiscordIntegration.CallbackServer;
import com.dod.UnrealZaruba.DiscordIntegration.DiscordAuth;
import com.dod.UnrealZaruba.Gamemodes.DestroyObjectivesGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.ScoreboardManager;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjectivesHandler;
import com.dod.UnrealZaruba.NetworkPackets.LoginPacket;
import com.dod.UnrealZaruba.Player.PlayerU;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.TeamLogic.TeamU;
import com.dod.UnrealZaruba.Utils.TickTimer;
import com.dod.UnrealZaruba.Utils.TimerManager;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;


import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Mod.EventBusSubscriber.Bus.FORGE)
// @Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class ServerEvents {

    private static TeamGamemode gamemode;
    private static TeamManager teamManager;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        CallbackServer.StartServer();
        TeamU.SetupMinecraftTeams(event.getServer());
        gamemode = new DestroyObjectivesGamemode();
        teamManager = gamemode.GetTeamManager();
        ScoreboardManager.clearScoreboard(event.getServer());
        GamemodeManager.StartGame(event.getServer().overworld(), gamemode);


        Path worldPath = Paths.get(event.getServer().getServerDirectory().toString(), "saves", "gamemode_world");

        try (ServerLevel level = WorldManager.LoadWorld(event.getServer(), worldPath)) 
        {
            if (level != null) {
                UnrealZaruba.LOGGER.warn("Custom world loaded successfully!");
            } else {
                UnrealZaruba.LOGGER.error("Failed to load the custom world.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == ServerTickEvent.Phase.START) {
            TimerManager.UpdateAll();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Scoreboard scoreboard = event.getServer().getScoreboard();

        scoreboard.removePlayerTeam(TeamU.redTeam);
        scoreboard.removePlayerTeam(TeamU.blueTeam);

        UnrealZaruba.LOGGER.info("Server has stopped. Finalizing...");
        DestructibleObjectivesHandler.Save();
        gamemode.GetTeamManager().Save();
        CallbackServer.StopServer();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ServerLifecycleHooks.getCurrentServer().isDedicatedServer())
            return;

        ServerPlayer player = (ServerPlayer) event.getPlayer();
        PlayerU playeru = PlayerU.Instantiate(player.getUUID(), player.gameMode.getGameModeForPlayer());
        playeru.SetGamemode(gamemode);

        TickTimer[] timer = new TickTimer[1];
        timer[0] = TimerManager.Create(30 * 60 * 20, () -> {
            if (playeru.IsAuthorized()) {
                UnrealZaruba.LOGGER.info("[INFOOO] Player disconected " + player.getName().getString());
                player.connection.disconnect(new TextComponent("Ну ты это, авторизуйся как бы. [30 sec]"));
            }
        }, 
        ticks -> {
            if (ticks % 100 == 0) {
                if (playeru.IsAuthorized()) {
                    player.sendMessage(new TextComponent("Не авторизован, войди в систему через дискорд"), player.getUUID());
                } else {
                    timer[0].Dispose(false);
                }
            }
        });

        gamemode.HandleConnectedPlayer(event.getPlayer());
        String state = UUID.randomUUID().toString();
        DiscordAuth.unresolvedRequests.add(state);
        UnrealZaruba.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new LoginPacket(state, player.getUUID(), player.getName().getString()));

    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!ServerLifecycleHooks.getCurrentServer().isDedicatedServer())
            return;

        PlayerU.Deauthorize(event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        UnrealZaruba.LOGGER.info("COMMANDS Registered");
        CommandRegistration.onCommandRegister(event);
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!PlayerU.Get(player.getUUID()).IsAuthorized()) {
                event.setCanceled(true);
                player.sendMessage(new TextComponent("You are not authorized to interact!"), player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!PlayerU.Get(player.getUUID()).IsAuthorized()) {
                event.setCanceled(true);
                player.sendMessage(new TextComponent("You are not authorized to attack!"), player.getUUID());
            }
        }
    }


    @SubscribeEvent
    public static void onPlayerBreakBlock(PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!PlayerU.Get(player.getUUID()).IsAuthorized()) {
                event.setCanceled(true);
                player.sendMessage(new TextComponent("You are not authorized to break blocks!"), player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!PlayerU.Get(player.getUUID()).IsAuthorized()) {
                event.setCanceled(true);
                player.sendMessage(new TextComponent("You are not authorized to place blocks!"), player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerItemPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!PlayerU.Get(event.getPlayer().getUUID()).IsAuthorized()) {
                event.setCanceled(true);
                player.sendMessage(new TextComponent("You are not authorized to pick up items!"), player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingUpdateEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();
            if (!PlayerU.Get(playerUUID).IsAuthorized()) {
                Vec3 prevPos = player.position();
                player.teleportTo(prevPos.x, prevPos.y, prevPos.z);
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0.0f;
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (!PlayerU.Get(player.getUUID()).IsAuthorized()) {
                event.setCanceled(true);
                event.getPlayer().sendMessage(new TextComponent("You are not authorized to break blocks!"),
                        event.getPlayer().getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!PlayerU.Get(player.getUUID()).IsAuthorized()) {
                event.setCanceled(true);
                player.sendMessage(new TextComponent("You are not authorized to place blocks!"), player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCommand(CommandEvent event) {
        ServerPlayer player = null;
        try {
            player = event.getParseResults().getContext().getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        if (player == null)
            return;
        if (!PlayerU.Get(player.getUUID()).IsAuthorized()) {
            event.setCanceled(true); // Cancel the command
            player.sendMessage(new TextComponent("You are not authorized to use commands!"), player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        event.setCanceled(true);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (!PlayerU.Get(event.getPlayer().getUUID()).IsAuthorized()) {
            event.getPlayer().sendMessage(new TextComponent("Chat is disabled"), event.getPlayer().getUUID());
            return;
        }

        TeamU teamU = teamManager.GetPlayersTeam(event.getPlayer());
        if (teamU != null) {
            for (var memberUUID : teamU.Members()) {
                ServerPlayer player = server.getPlayerList().getPlayer(memberUUID);
                if (player == null) continue;
                player.sendMessage(
                    new TextComponent("§4" + event.getPlayer().getName().getString()+ "§r" + ":" + event.getMessage()), memberUUID);
            }
        }
    }   

    @SubscribeEvent
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        if (!PlayerU.Get(event.getPlayer().getUUID()).IsAuthorized()) {
            event.setResult(PlayerSleepInBedEvent.Result.DENY);
            event.getPlayer().sendMessage(new TextComponent("You are not authorized to sleep!"),
                    event.getPlayer().getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!PlayerU.Get(player.getUUID()).IsAuthorized()) {
                event.setCanceled(true);
                player.sendMessage(new TextComponent("You are not authorized to attack!"), player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {    
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!PlayerU.Get(player.getUUID()).IsAuthorized()) {
                event.setCanceled(true);
                player.sendMessage(new TextComponent("You are not authorized to take damage!"), player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!PlayerU.Get(player.getUUID()).IsAuthorized()) {
                event.setCanceled(true);
                player.sendMessage(new TextComponent("You are not authorized to be knocked back!"), player.getUUID());
            }
        }
    }
}
