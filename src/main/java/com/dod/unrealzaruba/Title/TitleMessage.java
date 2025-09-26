package com.dod.unrealzaruba.Title;

import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;


/**
 * The type Title message.
 */
public class TitleMessage {

    /**
     * Send title.
     *
     * @deprecated
     *
     * @param player the player
     * @param title  the title
     */
    @Deprecated
    public static void sendTitle(ServerPlayer player, String title) {
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)) {
        });
        player.connection.send(new ClientboundSetTitlesAnimationPacket(7, 7, 7)); // fade in, stay, fade out
    }

    public static void sendSubtitle(ServerPlayer serverPlayer, Component subtitle) {
        serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(7, 7, 7));
    }

    public static void sendActionbar(ServerPlayer serverPlayer, Component message) {
        serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
        serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(7, 7, 7));
    }

    public static void sendActionbarToEveryone(MinecraftServer server, Component message) {
        for(var player : server.getPlayerList().getPlayers()) {
            sendActionbar(player, message);
        }
    }

    public static void showTitle(ServerPlayer player, Component title, Component subtitle) {
        showTitle(player, title, subtitle, 60);
    }

    /**
     * Show title.
     *
     * @param player       the player
     * @param title        the title
     * @param subtitle     the subtitle
     * @param tickduration the tickduration
     */
    public static void showTitle(ServerPlayer player, Component title, Component subtitle, int tickduration) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, tickduration, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }
}