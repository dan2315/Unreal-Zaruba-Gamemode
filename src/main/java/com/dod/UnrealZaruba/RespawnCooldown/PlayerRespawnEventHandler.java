package com.dod.UnrealZaruba.RespawnCooldown;

import com.dod.UnrealZaruba.Gamemodes.DestroyObjectivesGamemode;
import com.dod.UnrealZaruba.Gamemodes.GameStage;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import com.dod.UnrealZaruba.Utils.TimerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.event.entity.living.LivingDeathEvent;



@Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Bus.FORGE)
public class PlayerRespawnEventHandler{
    int start_time = 11;
    double deathX = 0;
    double deathY = 0;
    double deathZ = 0;

    @SubscribeEvent
    public void OnPlayerDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
            this.deathX = serverPlayer.getX();
            this.deathY = serverPlayer.getY();
            this.deathZ = serverPlayer.getZ();
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (BaseGamemode.currentGamemode.gameStage == GameStage.Preparation || BaseGamemode.currentGamemode.gameStage == GameStage.Battle){
            if (event.getPlayer() instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) event.getPlayer();
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(deathX, deathY, deathZ);

                var duration = 10;
                TimerManager.Create(duration * 1000
                        , () -> {
                            ServerPlayer serverPlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(player.getUUID());
                            if (serverPlayer != null) {
                                serverPlayer.setGameMode(GameType.ADVENTURE);
                                DestroyObjectivesGamemode.TeamManager.teleportToSpawn(serverPlayer);
                            }
                        },
                        ticks -> {
                            if (ticks % 20 != 0) return;
                            TitleMessage.sendTitle(player, "§4" + String.valueOf(duration - ticks / 20));
                        });
            }
        }
    }
}