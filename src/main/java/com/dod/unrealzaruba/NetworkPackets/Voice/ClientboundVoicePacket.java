package com.dod.unrealzaruba.NetworkPackets.Voice;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A voice line for the player to hear. It is played in the player's head, not in the world: no position, no fading with the distance, so it
 * does not drift to a side when they move or sit in a vehicle, and a new line cuts the one that is still playing.
 */
public class ClientboundVoicePacket implements ModPacket {
    private final ResourceLocation sound;

    public ClientboundVoicePacket(ResourceLocation sound) {
        this.sound = sound;
    }

    public static void encode(ClientboundVoicePacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.sound);
    }

    public static ClientboundVoicePacket decode(FriendlyByteBuf buf) {
        return new ClientboundVoicePacket(buf.readResourceLocation());
    }

    public static void handle(ClientboundVoicePacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> Playback.play(msg.sound));
        ctx.get().setPacketHandled(true);
    }

    /** Client side playback, kept apart so that the packet class itself stays free of client-only calls at load time. */
    private static final class Playback {
        private static SoundInstance current;

        static void play(ResourceLocation sound) {
            var manager = Minecraft.getInstance().getSoundManager();
            if (current != null) manager.stop(current);
            current = new SimpleSoundInstance(sound, SoundSource.VOICE, 1.0f, 1.0f, RandomSource.create(), false, 0,
                    SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
            manager.play(current);
        }
    }
}
