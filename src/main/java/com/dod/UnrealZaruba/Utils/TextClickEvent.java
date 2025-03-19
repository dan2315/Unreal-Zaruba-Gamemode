package com.dod.UnrealZaruba.Utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Clickable text in chat
 */
public class TextClickEvent {

    /**
     * Send clickable message.
     *
     * @param player       the player
     * @param message_text the message text
     * @param command      the command
     */
    public static void sendClickableMessage(ServerPlayer player, String message_text, String command) {
        // Create the text component
        MutableComponent clickableText = Component.literal(message_text);

        // Create the ClickEvent
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
        // Attach the ClickEvent to the text component
        clickableText.withStyle(style -> style.withClickEvent(clickEvent));

        // Optional: Set the color and formatting of the text
        clickableText.withStyle(style -> style.withColor(0x00FF00).withBold(true));

        // Send the message to the player
        player.sendSystemMessage(clickableText);
    }
}
