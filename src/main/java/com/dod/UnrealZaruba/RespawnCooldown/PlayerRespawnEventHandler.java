package com.dod.UnrealZaruba.RespawnCooldown;

import com.dod.UnrealZaruba.Gamemodes.GameStage;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import com.dod.UnrealZaruba.Utils.TimerManager;

import com.dod.UnrealZaruba.Utils.NBT;

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

    @SubscribeEvent
    public void OnPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayer))
            return;
        ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
        NBT.addEntityTag(serverPlayer, "isPlayerDead", 1);


        if (BaseGamemode.currentGamemode.gameStage != GameStage.Preparation) {
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
                    BaseGamemode.currentGamemode.TeamManager.teleportToSpawn(serverPlayer);
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