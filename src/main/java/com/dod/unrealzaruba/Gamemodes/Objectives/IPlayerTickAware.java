package com.dod.unrealzaruba.Gamemodes.Objectives;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;


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
    void onPlayerTick(PlayerTickEvent.Post event, ServerPlayer player);
} 