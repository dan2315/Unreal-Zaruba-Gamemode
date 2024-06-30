package com.dod.UnrealZaruba.RespawnCooldown;

import net.minecraft.core.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Bus.FORGE)
public class SimplePlayerRespawnEventHandler {

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {

        BlockPos spawn = ServerLifecycleHooks.getCurrentServer().overworld().getSharedSpawnPos();

        event.getPlayer().teleportTo(spawn.getX(), spawn.getX(), spawn.getX());

    }
}