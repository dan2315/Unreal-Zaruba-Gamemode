package com.dod.UnrealZaruba.Title;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;


public class TitleMessage {

    public static void sendTitle(ServerPlayer player, String title) {
        player.connection.send(new ClientboundSetTitleTextPacket(new TextComponent(title)));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 10, 10)); // fade in, stay, fade out
//        if (!subtitle.isEmpty()) {
//            player.connection.send(new ClientboundSetTitleTextPacket(new TextComponent(subtitle)));
//            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
//        }
    }

    public static void showTitle(ServerPlayer player, TextComponent title, TextComponent subtitle) {
        showTitle(player, title, subtitle, 60);
    }

    public static void showTitle(ServerPlayer player, TextComponent title, TextComponent subtitle, int tickduration) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, tickduration, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }
}