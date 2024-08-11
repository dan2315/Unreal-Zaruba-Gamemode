package com.dod.UnrealZaruba.RespawnCooldown;

import com.dod.UnrealZaruba.Gamemodes.GameStage;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.Player.PlayerU;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.dod.UnrealZaruba.SoundHandler.SoundHandler;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import com.dod.UnrealZaruba.Utils.TextClickEvent;
import com.dod.UnrealZaruba.Utils.TimerManager;

import com.dod.UnrealZaruba.Utils.NBT;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Bus.FORGE)
public class PlayerRespawnEventHandler {
    public static final HashMap<UUID, Boolean> DeadPlayers = new HashMap<>(); // false = team spawn | true = team tent

    @SubscribeEvent
    public void OnPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayer serverPlayer))
            return;
        
        BaseGamemode gamemode = GamemodeManager.Get(event.getEntity().level);
        
        if (gamemode.gameStage == GameStage.Preparation) return;
        TeamManager teamManager = ((TeamGamemode)gamemode).GetTeamManager();

        NBT.addEntityTag(serverPlayer, "isPlayerDead", 1);
        DeadPlayers.put(serverPlayer.getUUID(), false);
        SoundHandler.playSoundToPlayer(serverPlayer, ModSounds.DEATH.get(), 1.0f, 1.0f);
        if (event.getSource().getEntity() != null && event.getSource().getEntity() instanceof Player) {
            Entity killer_entity = event.getSource().getEntity();
            ServerPlayer killer_player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(killer_entity.getUUID());

            TitleMessage.sendActionbar(killer_player, new TextComponent("§c ☠ Вы убили " + serverPlayer.getName().getString() + " ☠"));
            TitleMessage.sendActionbar(serverPlayer, new TextComponent("§c ☠ Вас убил " + killer_player.getName().getString() + " ☠"));
        }

        serverPlayer.sendMessage(new TextComponent("====================="), serverPlayer.getUUID());
        TextClickEvent.sendClickableMessage(serverPlayer, "Возродиться на базе", "/tpToTeamSpawn");
        serverPlayer.sendMessage(new TextComponent(""), serverPlayer.getUUID());
        if (!(teamManager.GetPlayersTeam(serverPlayer).active_tent == null)) {
            TextClickEvent.sendClickableMessage(serverPlayer, "Возродиться в палатке", "/tpToTeamTent");
            serverPlayer.sendMessage(new TextComponent("====================="), serverPlayer.getUUID());
        }

        if (gamemode.gameStage != GameStage.Preparation) {
            if (serverPlayer instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) serverPlayer;
                ServerLevel serverWorld = player.getLevel();
                serverWorld.getServer().execute(() -> {
                    player.setGameMode(GameType.SPECTATOR);
                });

                var duration = 10;
                TimerManager.Create(duration * 1000, () -> {
                    serverPlayer.setGameMode(GameType.ADVENTURE);
                    NBT.addEntityTag(serverPlayer, "isPlayerDead", 0);
                    teamManager.RespawnPlayer(player, DeadPlayers.get(serverPlayer.getUUID()));

                    SoundHandler.playSoundToPlayer(serverPlayer, ModSounds.RESPAWN2.get(), 1.0f, 1.0f);
                },
                        ticks -> {
                            if (ticks % 20 != 0)
                                return;
                            TitleMessage.sendTitle(player, "§4" + String.valueOf(duration - ticks / 20));
                        });
            }
        }

        serverPlayer.setHealth(20.0F);
        event.setCanceled(true);
    }
}