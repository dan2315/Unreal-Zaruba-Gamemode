package com.dod.UnrealZaruba.Utils;


import com.dod.UnrealZaruba.unrealzaruba;

import net.minecraftforge.event.server.ServerStartingEvent;

public class MixinTest {
    public void aboba(ServerStartingEvent event) {
        unrealzaruba.LOGGER.info("Я ебу Али бабу");
    }
}
