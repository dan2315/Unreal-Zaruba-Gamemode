package com.dod.UnrealZaruba.DiscordIntegration;

import java.util.UUID;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlayerInteractionBlocker {

    @SubscribeEvent
        public static void onPlayerInteract(PlayerInteractEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!CallbackServer.isPlayerAuthorized(player.getUUID())) {
                    event.setCanceled(true);
                    player.sendMessage(new TextComponent("You are not authorized to interact!"), player.getUUID());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerAttack(AttackEntityEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!CallbackServer.isPlayerAuthorized(player.getUUID())) {
                    event.setCanceled(true);
                    player.sendMessage(new TextComponent("You are not authorized to attack!"), player.getUUID());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerUseItem(PlayerInteractEvent.RightClickItem event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!CallbackServer.isPlayerAuthorized(player.getUUID())) {
                    event.setCanceled(true);
                    player.sendMessage(new TextComponent("You are not authorized to use items!"), player.getUUID());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerBreakBlock(PlayerEvent.BreakSpeed event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!CallbackServer.isPlayerAuthorized(player.getUUID())) {
                    event.setCanceled(true);
                    player.sendMessage(new TextComponent("You are not authorized to break blocks!"), player.getUUID());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerPlaceBlock(BlockEvent.EntityPlaceEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!CallbackServer.isPlayerAuthorized(player.getUUID())) {
                    event.setCanceled(true);
                    player.sendMessage(new TextComponent("You are not authorized to place blocks!"), player.getUUID());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerItemPickup(EntityItemPickupEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!CallbackServer.isPlayerAuthorized(player.getUUID())) {
                    event.setCanceled(true);
                    player.sendMessage(new TextComponent("You are not authorized to pick up items!"), player.getUUID());
                }
            }
        }

    @SubscribeEvent
    public static void onLivingUpdate(LivingUpdateEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();
            if (!CallbackServer.isPlayerAuthorized(playerUUID)) {
                // Reset player position to the previous tick's position
                Vec3 prevPos = player.position();
                player.teleportTo(prevPos.x, prevPos.y, prevPos.z);
                player.setDeltaMovement(Vec3.ZERO); // Set velocity to zero to prevent further movement
                player.fallDistance = 0.0f; // Reset fall distance to prevent fall damage
                player.sendMessage(new TextComponent("You are not authorized to asd!"), playerUUID);
                
                // GameType originalGameMode = playerGameModeMap.get(playerUUID);
                // if (originalGameMode != null && player.gameMode.getGameModeForPlayer() != originalGameMode) {
                //     player.setGameMode(originalGameMode);
                //     player.sendMessage(new TextComponent("You are not authorized to change game mode!"), playerUUID);
                // }
            }
        }
    }

        @SubscribeEvent
        public static void onBlockBreak(BreakEvent event) {
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onBlockPlace(EntityPlaceEvent event) {
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onPlayerChat(ServerChatEvent event) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("Chat is disabled"), event.getPlayer().getUUID());
        }

        @SubscribeEvent
        public static void onPlayerSleep(PlayerSleepInBedEvent event) {
            event.setResult(PlayerSleepInBedEvent.Result.DENY);
        }

        @SubscribeEvent
        public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
            if (event.getEntity() instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) event.getEntity();
                player.sendMessage(new TextComponent("All interactions are disabled"), player.getUUID());
            }
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!CallbackServer.isPlayerAuthorized(player.getUUID())) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!CallbackServer.isPlayerAuthorized(player.getUUID())) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onLivingKnockBack(LivingKnockBackEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!CallbackServer.isPlayerAuthorized(player.getUUID())) { 
                    event.setCanceled(true);
                }
            }
        }
    }

