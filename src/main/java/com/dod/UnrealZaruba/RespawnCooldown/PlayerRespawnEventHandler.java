package com.dod.UnrealZaruba.RespawnCooldown;

import com.dod.UnrealZaruba.Gamemodes.GameStage;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import com.dod.UnrealZaruba.Utils.TimerManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

@Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Bus.FORGE)
public class PlayerRespawnEventHandler {
    int start_time = 11;
    double deathX = 0;
    double deathY = 0;
    double deathZ = 0;

    @SubscribeEvent
    public void OnPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayer))
            return;
        ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();

        if (event.getEntityLiving() instanceof ServerPlayer) {
            this.deathX = serverPlayer.getX();
            this.deathY = serverPlayer.getY();
            this.deathZ = serverPlayer.getZ();
        }

        if (BaseGamemode.currentGamemode.gameStage != GameStage.Preparation) {
            if (serverPlayer instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) serverPlayer;
                ServerLevel serverWorld = player.getLevel();
                serverWorld.getServer().execute(() -> {
                    player.teleportTo(deathX, deathY, deathZ);
                    player.setGameMode(GameType.SPECTATOR);
                });

                var duration = 10;
                TimerManager.Create(duration * 1000, () -> {
                    if (serverPlayer != null) {
                        serverPlayer.setGameMode(GameType.ADVENTURE);
                        BaseGamemode.currentGamemode.TeamManager.teleportToSpawn(serverPlayer);
                    }
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