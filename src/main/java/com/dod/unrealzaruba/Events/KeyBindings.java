package com.dod.unrealzaruba.Events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.unrealzaruba";
    /**
     * The lobby keys work only while the lobby voting is on, and conflict with nothing else: G and R are also TACZ's fire
     * mode and reload, which is no use in the lobby, and these keys are no use in a match.
     */
    public static final net.neoforged.neoforge.client.settings.IKeyConflictContext LOBBY = new net.neoforged.neoforge.client.settings.IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return com.dod.unrealzaruba.UI.Lobby.LobbyClientState.active();
        }

        @Override
        public boolean conflicts(net.neoforged.neoforge.client.settings.IKeyConflictContext other) {
            return other == this;
        }
    };

    public static final KeyMapping OPEN_GAMEMODE_MENU = new KeyMapping(
            "key.unrealzaruba.open_gamemode_menu",
            LOBBY,
            InputConstants.Type.KEYSYM,
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

    public static final KeyMapping READY = new KeyMapping(
            "key.unrealzaruba.ready",
            LOBBY,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            CATEGORY
    );

    /** Held beside a vehicle that lies on its roof: after ten seconds it is lifted and turned onto its wheels. */
    public static final KeyMapping FLIP_VEHICLE = new KeyMapping(
            "key.unrealzaruba.flip_vehicle",
            InputConstants.KEY_RBRACKET,
            CATEGORY
    );

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GAMEMODE_MENU);
        event.register(OPEN_BUY_MENU);
        event.register(OPEN_TEAM_STORAGE);
        event.register(OPEN_SCHEMATICS);
        event.register(READY);
        event.register(FLIP_VEHICLE);
    }
}
