package com.dod.unrealzaruba.SoundHandler;

import java.util.function.Predicate;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Feed.Feed;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/** Plays the voice lines ({@link VoiceLine}) to the players they are for. */
public final class Voice {
    private Voice() {}

    public static void playAll(MinecraftServer server, VoiceLine line) {
        play(server, line, player -> true);
    }

    /**
     * One line for the team the event is about and another for the others (players in no team hear the "enemy" one).
     * Either line may be null for nobody.
     */
    public static void playSplit(MinecraftServer server, TeamColor subject, VoiceLine ally, VoiceLine enemy) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            VoiceLine line = Feed.teamOf(player) == subject && subject != null ? ally : enemy;
            if (line != null) send(player, line);
        }
    }

    /** A line picked for every player; null for nobody. */
    public static void playEach(MinecraftServer server, java.util.function.Function<ServerPlayer, VoiceLine> pick) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            VoiceLine line = pick.apply(player);
            if (line != null) send(player, line);
        }
    }

    public static void play(MinecraftServer server, VoiceLine line, Predicate<ServerPlayer> who) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (who.test(player)) send(player, line);
        }
    }

    private static void send(ServerPlayer player, VoiceLine line) {
        var sound = ModSounds.voice(line);
        if (sound != null) player.playNotifySound(sound.get(), SoundSource.VOICE, 1.0f, 1.0f);
    }
}
