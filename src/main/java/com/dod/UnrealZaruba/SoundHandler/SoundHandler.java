package com.dod.UnrealZaruba.SoundHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;


/**
 * The type Sound handler.
 */
public class SoundHandler {

    /**
     * Play sound to player.
     *
     * @param player the player
     * @param sound  the sound
     * @param volume the volume
     * @param pitch  the pitch
     */
    public static void playSoundToPlayer(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.playNotifySound(sound, player.getSoundSource(), volume, pitch);
    }

    /**
     * Play sound from position.
     *
     * @param level    the level
     * @param pos      the pos
     * @param sound    the sound
     * @param category the category
     * @param volume   the volume
     * @param pitch    the pitch
     */
    public static void playSoundFromPosition(ServerLevel level, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch) {
        level.playSound(null, pos, sound, category, volume, pitch);
    }

//    @Mod.EventBusSubscriber(modid = "unrealzaruba")
//    public static class OnPlayerLoginEventHandler {
//
//        @SubscribeEvent
//        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
//            if (event.getPlayer() instanceof ServerPlayer) {
//                ServerPlayer player = (ServerPlayer) event.getPlayer();
//                playSound(player, ModSounds.HORN_DIRE, player.position(), SoundSource.PLAYERS, 1.0F, 1.0F);
//                  playSoundToPlayer(player, ModSounds.HORN_DIRE.get(), 0.2F, 1.0F);
//                  player.sendMessage(new TextComponent("Sound Test Passed"), player.getUUID());
//            }
//        }
//    }
}