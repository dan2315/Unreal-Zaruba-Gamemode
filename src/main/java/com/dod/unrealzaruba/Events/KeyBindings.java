package com.dod.unrealzaruba.Events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.unrealzaruba";
    public static final KeyMapping OPEN_GAMEMODE_MENU = new KeyMapping(
      "key.unrealzaruba.open_gamemode_menu",
            InputConstants.KEY_G,
            CATEGORY
    );
    public static final KeyMapping OPEN_BUY_MENU = new KeyMapping(
            "key.unrealzaruba.open_buy_menu",
            InputConstants.KEY_B,
            CATEGORY
    );
    public static final KeyMapping OPEN_TEAM_STORAGE = new KeyMapping(
            "key.unrealzaruba.open_team_storage",
            InputConstants.KEY_H,
            CATEGORY
    );
    public static final KeyMapping OPEN_SCHEMATICS = new KeyMapping(
            "key.unrealzaruba.open_schematics",
            InputConstants.KEY_K,
            CATEGORY
    );

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GAMEMODE_MENU);
        event.register(OPEN_BUY_MENU);
        event.register(OPEN_TEAM_STORAGE);
        event.register(OPEN_SCHEMATICS);
    }
}
