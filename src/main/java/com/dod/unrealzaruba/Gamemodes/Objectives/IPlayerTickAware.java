package com.dod.unrealzaruba.Gamemodes.Objectives;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

/**
 * Interface for objects that need to receive player tick events.
 */
public interface IPlayerTickAware {
    /**
     * Called on each player tick
     * 
     * @param event The player tick event
     * @param player The server player
     */
    void onPlayerTick(TickEvent.PlayerTickEvent event, ServerPlayer player);
} 