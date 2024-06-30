package com.dod.UnrealZaruba.SoundHandler;

import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


public class SoundHandler {

    public static void playSound(ServerPlayer player, SoundEvent sound, Vec3 position, SoundSource category, float volume, float pitch) {
        ServerLevel serverWorld = player.getLevel();
        serverWorld.playSound(null, position.x, position.y, position.z, sound, category, volume, pitch);
    }

    @Mod.EventBusSubscriber(modid = "unrealzaruba")
    public static class OnPlayerLoginEventHandler {

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getPlayer() instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) event.getPlayer();
                playSound(player, ModSounds.horn_radiant, player.position(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }
}