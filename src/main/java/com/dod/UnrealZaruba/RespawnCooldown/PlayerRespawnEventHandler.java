package com.dod.UnrealZaruba.RespawnCooldown;

import com.dod.UnrealZaruba.Gamemodes.DestroyObjectivesGamemode;
import com.dod.UnrealZaruba.Title.TitleMessage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.event.entity.living.LivingDeathEvent;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Bus.FORGE)
public class PlayerRespawnEventHandler {
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
        if (event.getPlayer() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            player.setGameMode(GameType.SPECTATOR);
            player.teleportTo(deathX, deathY, deathZ);
            player.sendMessage(new TextComponent("§4Вы мертвы. . ."), player.getUUID());

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            Runnable task = () -> {
                //Pray to God this code works
                this.start_time = this.start_time - 1;
                TitleMessage.sendTitle(player, String.valueOf("§4" + this.start_time));
                System.out.println("Task executed at " + System.currentTimeMillis());
                if (this.start_time < 0){
                    scheduler.shutdown();
                    this.start_time = 11;
                }
            };

            // Schedule the task to run every 5 seconds with an initial delay of 0 seconds
            scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);

            scheduler.schedule(() -> {
                ServerPlayer serverPlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(player.getUUID());
                if (serverPlayer != null) {
//                  serverPlayer.teleportTo(player.getRespawnPosition().getX(), player.getRespawnPosition().getY(), player.getRespawnPosition().getZ());
                    serverPlayer.setGameMode(GameType.SURVIVAL);
                    DestroyObjectivesGamemode.TeamManager.teleportToSpawn(serverPlayer);
                    serverPlayer.sendMessage(new TextComponent("Вы возродились."), serverPlayer.getUUID());
                }
                scheduler.shutdown();
                this.start_time = 11;
            }, 10, TimeUnit.SECONDS); // stops the scheduler after 10 seconds
        }
    }
}