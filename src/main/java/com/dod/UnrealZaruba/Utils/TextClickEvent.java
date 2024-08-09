package com.dod.UnrealZaruba.Utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.MinecraftServer;


public class TextClickEvent {

    public static void sendClickableMessage(ServerPlayer player, String message_text, String command) {
        // Create the text component
        MutableComponent clickableText = new TextComponent(message_text);

        // Create the ClickEvent
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
        // Attach the ClickEvent to the text component
        clickableText.withStyle(style -> style.withClickEvent(clickEvent));

        // Optional: Set the color and formatting of the text
        clickableText.withStyle(style -> style.withColor(0x00FF00).withBold(true));

        // Send the message to the player
        player.sendMessage(clickableText, player.getUUID());


    }
}
