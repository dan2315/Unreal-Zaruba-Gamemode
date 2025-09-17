package com.dod.UnrealZaruba.Events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.unrealzaruba";
    public static final KeyMapping OPEN_GAMEMODE_MENU = new KeyMapping(
      "key.unrealzaruba.open_gamemode_menu",
            InputConstants.KEY_G,
            CATEGORY
    );

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GAMEMODE_MENU);
    }
}
