package com.dod.UnrealZaruba.Utils;


import com.dod.UnrealZaruba.UnrealZaruba;

import net.minecraftforge.event.server.ServerStartingEvent;

public class MixinTest {
    public void aboba(ServerStartingEvent event) {
        UnrealZaruba.LOGGER.info("Я ебу Али бабу");
    }
}
