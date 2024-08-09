package com.dod.UnrealZaruba.Title;

import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;


public class TitleMessage {

    public static void sendTitle(ServerPlayer player, String title) {
        player.connection.send(new ClientboundSetTitleTextPacket(new TextComponent(title)));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(7, 7, 7)); // fade in, stay, fade out
//        if (!subtitle.isEmpty()) {
//            player.connection.send(new ClientboundSetTitleTextPacket(new TextComponent(subtitle)));
//            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
//        }
    }

    public static void sendSubtitle(ServerPlayer serverPlayer, TextComponent subtitle) {
        serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(7, 7, 7));
    }

    public static void sendActionbar(ServerPlayer serverPlayer, TextComponent action) {
        serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(action));
        serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(7, 7, 7));
    }

    public static void showTitle(ServerPlayer player, TextComponent title, TextComponent subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(7, 7, 7));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }
}